package dev.doorelauncher;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Microsoft sign-in for Minecraft: the device-code flow (the player opens a page and types a short code), then Xbox Live,
 * XSTS and the Minecraft services. Only an account that owns Minecraft Java Edition gets a session.
 */
final class Auth {
    private static final String TENANT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";
    private static final String SCOPE = "XboxLive.signin offline_access";

    record Session(String name, String uuid, String accessToken, String refreshToken) {
    }

    private Auth() {
    }

    /** Shows the player a code and a page, waits until the code is entered, and returns the session. */
    static Session signIn(String clientId, Consumer<String[]> showCode) throws IOException, InterruptedException {
        Http.Reply r = Http.postForm(TENANT + "devicecode", form("client_id", clientId, "scope", SCOPE));
        Map<String, Object> code = Json.obj(Json.parse(r.body()));
        if (r.status() != 200) {
            throw new IOException(explain(code));
        }
        showCode.accept(new String[] {Json.str(code.get("user_code")), Json.str(code.get("verification_uri"))});
        long interval = Math.max(2L, Json.num(code.get("interval")));
        long until = System.currentTimeMillis() + Json.num(code.get("expires_in")) * 1000L;
        while (System.currentTimeMillis() < until) {
            Thread.sleep(interval * 1000L);
            Http.Reply t = Http.postForm(TENANT + "token", form(
                "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "client_id", clientId, "device_code", Json.str(code.get("device_code"))));
            Map<String, Object> token = Json.obj(Json.parse(t.body()));
            if (t.status() == 200) {
                return minecraft(Json.str(token.get("access_token")), Json.str(token.get("refresh_token")));
            }
            String error = Json.str(token.get("error"));
            if (error.equals("authorization_pending")) {
                continue;
            }
            if (error.equals("slow_down")) {
                interval += 5L;
                continue;
            }
            throw new IOException(explain(token));
        }
        throw new IOException("Код входа истёк, попробуй ещё раз.");
    }

    /** A new session from the refresh token kept after an earlier sign-in. */
    static Session refresh(String clientId, String refreshToken) throws IOException {
        Http.Reply t = Http.postForm(TENANT + "token", form(
            "grant_type", "refresh_token", "client_id", clientId, "refresh_token", refreshToken, "scope", SCOPE));
        Map<String, Object> token = Json.obj(Json.parse(t.body()));
        if (t.status() != 200) {
            throw new IOException(explain(token));
        }
        String next = Json.str(token.get("refresh_token"));
        return minecraft(Json.str(token.get("access_token")), next.isEmpty() ? refreshToken : next);
    }

    private static Session minecraft(String msAccess, String refreshToken) throws IOException {
        Map<String, Object> xblProps = new LinkedHashMap<>();
        xblProps.put("AuthMethod", "RPS");
        xblProps.put("SiteName", "user.auth.xboxlive.com");
        xblProps.put("RpsTicket", "d=" + msAccess);
        Map<String, Object> xblBody = new LinkedHashMap<>();
        xblBody.put("Properties", xblProps);
        xblBody.put("RelyingParty", "http://auth.xboxlive.com");
        xblBody.put("TokenType", "JWT");
        Map<String, Object> xbl = checked(Http.postJson("https://user.auth.xboxlive.com/user/authenticate", Json.write(xblBody)), "Xbox Live");
        String xblToken = Json.str(xbl.get("Token"));
        String uhs = Json.str(Json.obj(Json.list(Json.obj(xbl.get("DisplayClaims")).get("xui")).get(0)).get("uhs"));

        Map<String, Object> xstsProps = new LinkedHashMap<>();
        xstsProps.put("SandboxId", "RETAIL");
        xstsProps.put("UserTokens", List.of(xblToken));
        Map<String, Object> xstsBody = new LinkedHashMap<>();
        xstsBody.put("Properties", xstsProps);
        xstsBody.put("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.put("TokenType", "JWT");
        Http.Reply xr = Http.postJson("https://xsts.auth.xboxlive.com/xsts/authorize", Json.write(xstsBody));
        Map<String, Object> xsts = Json.obj(Json.parse(xr.body()));
        if (xr.status() != 200) {
            long err = Json.num(xsts.get("XErr"));
            if (err == 2148916233L) {
                throw new IOException("У этого аккаунта Microsoft нет профиля Xbox. Войди на minecraft.net и создай профиль.");
            }
            if (err == 2148916238L) {
                throw new IOException("Детский аккаунт: родитель должен добавить его в семью Microsoft.");
            }
            throw new IOException("XSTS: ошибка " + err);
        }

        Map<String, Object> login = new LinkedHashMap<>();
        login.put("identityToken", "XBL3.0 x=" + uhs + ";" + Json.str(xsts.get("Token")));
        Map<String, Object> mc = checked(Http.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", Json.write(login)), "Minecraft");
        String access = Json.str(mc.get("access_token"));

        String bearer = "Bearer " + access;
        Map<String, Object> items = checked(Http.get("https://api.minecraftservices.com/entitlements/mcstore", "Authorization", bearer), "покупки");
        Http.Reply pr = Http.get("https://api.minecraftservices.com/minecraft/profile", "Authorization", bearer);
        if (pr.status() == 404 || Json.list(items.get("items")).isEmpty() && pr.status() != 200) {
            throw new IOException("На этом аккаунте нет Minecraft Java Edition.");
        }
        Map<String, Object> profile = checked(pr, "профиль");
        return new Session(Json.str(profile.get("name")), Json.str(profile.get("id")), access, refreshToken);
    }

    private static Map<String, Object> checked(Http.Reply r, String what) throws IOException {
        if (r.status() / 100 != 2) {
            throw new IOException("Не удалось войти (" + what + ", код " + r.status() + ").");
        }
        return Json.obj(Json.parse(r.body()));
    }

    private static String explain(Map<String, Object> error) {
        String text = Json.str(error.get("error_description"));
        if (text.contains("AADSTS700016") || text.contains("AADSTS70002") || text.contains("invalid_client")) {
            return "Неверный Client ID приложения Microsoft. Проверь его в настройках лаунчера.";
        }
        return text.isEmpty() ? Json.str(error.get("error")) : text.split("\n")[0];
    }

    private static Map<String, String> form(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int k = 0; k + 1 < pairs.length; k += 2) {
            m.put(pairs[k], pairs[k + 1]);
        }
        return m;
    }
}
