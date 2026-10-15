# DooreLauncher

Собственный лаунчер DooreVisuals: сам скачивает Minecraft 1.21.11, Fabric Loader и Java с официальных серверов
(Mojang, Fabric) в папку `%APPDATA%\.doorelauncher`, ставит мод и запускает игру. Официальный лаунчер не нужен.

## Вход

Вход только через аккаунт Microsoft с купленным Minecraft Java Edition (flow «код на странице»: лаунчер показывает
короткий код, открывает страницу, игрок вводит код). Отдельные пароли лаунчер не хранит, только токен обновления
в `%APPDATA%\.doorelauncher\settings.json`.

Для входа нужен **Client ID** приложения Microsoft (один раз, бесплатно):

1. <https://portal.azure.com> → Microsoft Entra ID → App registrations → New registration.
   Тип аккаунтов: «Personal Microsoft accounts only» (или «Accounts in any ... and personal»).
2. В разделе Authentication включи **Allow public client flows** (Yes) и сохрани.
3. Скопируй **Application (client) ID**.
4. Mojang разрешает приложению обращаться к Minecraft API только после проверки: заполни форму
   <https://aka.ms/AppRegInfo> с этим Client ID. Пока заявка не одобрена, вход будет отвечать ошибкой.
5. Вставь Client ID в `launcher.json` (`msClientId`) перед сборкой или в «Настройки» лаунчера.

## Мод

Рядом с лаунчером лежит `doorevisuals-*.jar`, он копируется в `%APPDATA%\.doorelauncher\game\mods`.
При каждом запуске лаунчер смотрит последний релиз на GitHub (`zxchores/DooreVisuals`, файл `doorevisuals-X.Y.Z.jar`)
и скачивает его, если он новее. Fabric API внутри мода.

## Сборка

`build.cmd` (JDK 21+). Результат — папка `out\DooreLauncher` с `DooreLauncher.exe` и своей Java; её и архивируй.
