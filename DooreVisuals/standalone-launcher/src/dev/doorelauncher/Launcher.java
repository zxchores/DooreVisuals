package dev.doorelauncher;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/** The DooreVisuals launcher: play, accounts, news, the game console and settings. Sign-in is always through Microsoft. */
public final class Launcher {
    private static final String[] TABS = {"Играть", "Аккаунты", "Новости", "Консоль", "Настройки"};
    private static final Color[] ACCENTS = {
        new Color(0x8B7BD8), new Color(0x5BA8F0), new Color(0x4FD1B5), new Color(0xE8718D), new Color(0xF0B45B), new Color(0xB5E85B)
    };

    private final Path appDir;
    private final Map<String, Object> settings = new LinkedHashMap<>();
    private final Path settingsFile = Game.HOME.resolve("settings.json");

    private final JFrame frame = new JFrame("DooreVisuals");
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final List<Ui.Nav> navs = new ArrayList<>();

    private final JLabel accountLabel = Ui.label(" ", 14, Ui.MUTED, Font.PLAIN);
    private final JLabel status = Ui.label(" ", 13, Ui.MUTED, Font.PLAIN);
    private final Ui.Bar bar = new Ui.Bar();
    private Ui.Btn playBtn;
    private JTextField serverField;
    private final JPanel accountList = new JPanel();
    private final JLabel accountStatus = Ui.label(" ", 13, Ui.MUTED, Font.PLAIN);
    private final JTextArea console = new JTextArea();
    private final JTextArea news = new JTextArea();

    private volatile boolean busy;

    private Launcher(Path appDir) {
        this.appDir = appDir;
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Path app = Path.of(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        SwingUtilities.invokeLater(() -> new Launcher(app).show());
    }

    // ---- settings ----

    private String text(String key, String fallback) {
        Object v = settings.get(key);
        return v == null ? fallback : v.toString();
    }

    private int whole(String key, int fallback) {
        Object v = settings.get(key);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    private boolean flag(String key, boolean fallback) {
        Object v = settings.get(key);
        return v instanceof Boolean b ? b : fallback;
    }

    private String clientId() {
        return text("msClientId", "").trim();
    }

    private void load() {
        try {
            Path bundled = appDir.resolve("launcher.json");
            if (Files.isRegularFile(bundled)) {
                settings.putAll(Json.obj(Json.parse(Files.readString(bundled))));
            }
            if (Files.isRegularFile(settingsFile)) {
                settings.putAll(Json.obj(Json.parse(Files.readString(settingsFile))));
            }
        } catch (Exception ignored) {
            // Defaults.
        }
        try {
            Ui.accent = Color.decode(text("accent", "#8B7BD8"));
        } catch (NumberFormatException ignored) {
            // Default accent.
        }
    }

    private void save() {
        try {
            Files.createDirectories(Game.HOME);
            Files.writeString(settingsFile, Json.write(settings));
        } catch (IOException ignored) {
            // The settings are only lost for the next start.
        }
    }

    private List<Map<String, Object>> accounts() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : Json.list(settings.get("accounts"))) {
            out.add(Json.obj(o));
        }
        return out;
    }

    private void storeAccounts(List<Map<String, Object>> list) {
        settings.put("accounts", new ArrayList<Object>(list));
        save();
    }

    private Map<String, Object> selectedAccount() {
        List<Map<String, Object>> list = accounts();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(Math.max(0, Math.min(whole("selected", 0), list.size() - 1)));
    }

    // ---- window ----

    private void show() {
        load();
        Ui.Backdrop root = new Ui.Backdrop();
        root.setLayout(new BorderLayout());
        root.add(titleBar(), BorderLayout.NORTH);
        root.add(sidebar(), BorderLayout.WEST);

        cardPanel.setOpaque(false);
        cardPanel.add(playPage(), TABS[0]);
        cardPanel.add(accountsPage(), TABS[1]);
        cardPanel.add(newsPage(), TABS[2]);
        cardPanel.add(consolePage(), TABS[3]);
        cardPanel.add(settingsPage(), TABS[4]);
        root.add(cardPanel, BorderLayout.CENTER);

        frame.setUndecorated(true);
        frame.setContentPane(root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 620);
        frame.setShape(new RoundRectangle2D.Double(0, 0, 1000, 620, 22, 22));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Ui.startClock(root);

        tab(0);
        refreshAccounts();
        loadNews();
    }

    private JComponent titleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(8, 18, 0, 10));
        bar.setPreferredSize(new Dimension(10, 40));
        JLabel title = Ui.label("DooreVisuals Launcher", 12, Ui.MUTED, Font.PLAIN);
        bar.add(title, BorderLayout.WEST);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(windowButton("—", () -> frame.setState(Frame.ICONIFIED)));
        buttons.add(windowButton("✕", () -> System.exit(0)));
        bar.add(buttons, BorderLayout.EAST);
        Point[] origin = new Point[1];
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origin[0] = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = frame.getLocation();
                frame.setLocation(p.x + e.getX() - origin[0].x, p.y + e.getY() - origin[0].y);
            }
        };
        bar.addMouseListener(drag);
        bar.addMouseMotionListener(drag);
        return bar;
    }

    private JLabel windowButton(String glyph, Runnable action) {
        JLabel l = Ui.label(glyph, 15, Ui.MUTED, Font.PLAIN);
        l.setBorder(new EmptyBorder(0, 8, 0, 8));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                l.setForeground(Ui.TEXT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                l.setForeground(Ui.MUTED);
            }
        });
        return l;
    }

    private JComponent sidebar() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(14, 0, 18, 0));
        side.setPreferredSize(new Dimension(200, 10));
        JLabel logo = Ui.label("Doore", 26, Ui.TEXT, Font.BOLD);
        logo.setBorder(new EmptyBorder(0, 28, 20, 0));
        side.add(logo);
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            Ui.Nav nav = new Ui.Nav(TABS[i], () -> tab(index));
            navs.add(nav);
            side.add(nav);
        }
        side.add(Box.createVerticalGlue());
        JLabel version = Ui.label("Minecraft " + Game.MC + " · Fabric", 12, Ui.MUTED, Font.PLAIN);
        version.setBorder(new EmptyBorder(0, 28, 0, 0));
        side.add(version);
        return side;
    }

    private void tab(int index) {
        cards.show(cardPanel, TABS[index]);
        for (int i = 0; i < navs.size(); i++) {
            navs.get(i).select(i == index);
        }
    }

    private JPanel page() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(26, 30, 30, 40));
        return p;
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(0.0F);
        return c;
    }

    // ---- pages ----

    private JComponent playPage() {
        JPanel p = page();
        p.add(Box.createVerticalStrut(30));
        p.add(left(Ui.label("DooreVisuals", 58, Ui.TEXT, Font.BOLD)));
        p.add(left(Ui.label("Minecraft " + Game.MC + " с клиентом DooreVisuals", 15, Ui.MUTED, Font.PLAIN)));
        p.add(Box.createVerticalStrut(34));
        p.add(left(accountLabel));
        p.add(Box.createVerticalStrut(18));
        p.add(left(Ui.label("Сервер для автовхода (необязательно)", 12, Ui.MUTED, Font.PLAIN)));
        p.add(Box.createVerticalStrut(6));
        serverField = Ui.field(text("server", ""));
        serverField.setMaximumSize(new Dimension(340, 38));
        serverField.setPreferredSize(new Dimension(340, 38));
        left(serverField);
        p.add(serverField);
        p.add(Box.createVerticalStrut(22));
        playBtn = new Ui.Btn("Играть", true).onClick(this::onPlay);
        p.add(left(playBtn));
        p.add(Box.createVerticalStrut(20));
        left(bar);
        bar.setMaximumSize(new Dimension(340, 8));
        bar.setPreferredSize(new Dimension(340, 8));
        p.add(bar);
        p.add(Box.createVerticalStrut(10));
        p.add(left(status));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent accountsPage() {
        JPanel p = page();
        p.add(left(Ui.label("Аккаунты", 30, Ui.TEXT, Font.BOLD)));
        p.add(left(Ui.label("Вход только через Microsoft; нужна купленная Minecraft Java Edition", 13, Ui.MUTED, Font.PLAIN)));
        p.add(Box.createVerticalStrut(18));
        accountList.setOpaque(false);
        accountList.setLayout(new BoxLayout(accountList, BoxLayout.Y_AXIS));
        left(accountList);
        p.add(accountList);
        p.add(Box.createVerticalStrut(12));
        p.add(left(new Ui.Btn("+ Добавить аккаунт", false).onClick(this::addAccount)));
        p.add(Box.createVerticalStrut(12));
        p.add(left(accountStatus));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent newsPage() {
        JPanel p = page();
        p.add(left(Ui.label("Новости", 30, Ui.TEXT, Font.BOLD)));
        p.add(Box.createVerticalStrut(14));
        p.add(left(scroll(news, false)));
        return p;
    }

    private JComponent consolePage() {
        JPanel p = page();
        p.add(left(Ui.label("Консоль игры", 30, Ui.TEXT, Font.BOLD)));
        p.add(Box.createVerticalStrut(14));
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        Ui.Btn clear = new Ui.Btn("Очистить", false).onClick(() -> console.setText(""));
        Ui.Btn copy = new Ui.Btn("Копировать", false).onClick(() ->
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(console.getText()), null));
        row.add(clear);
        row.add(Box.createHorizontalStrut(10));
        row.add(copy);
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(left(row));
        p.add(Box.createVerticalStrut(12));
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(left(scroll(console, true)));
        return p;
    }

    private JScrollPane scroll(JTextArea area, boolean mono) {
        area.setEditable(false);
        area.setLineWrap(!mono);
        area.setWrapStyleWord(true);
        area.setOpaque(true);
        area.setBackground(new Color(0x14101F));
        area.setForeground(mono ? new Color(0xCFCBE6) : Ui.TEXT);
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        if (!mono) {
            area.setFont(Ui.font(Font.PLAIN, 14));
        }
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 24)));
        sp.getViewport().setBackground(new Color(0x14101F));
        return sp;
    }

    private JComponent settingsPage() {
        JPanel p = page();
        p.add(left(Ui.label("Настройки", 30, Ui.TEXT, Font.BOLD)));
        p.add(Box.createVerticalStrut(16));

        p.add(left(Ui.label("Client ID приложения Microsoft (нужен для входа, инструкция в README)", 12, Ui.MUTED, Font.PLAIN)));
        p.add(Box.createVerticalStrut(6));
        JTextField id = sized(Ui.field(clientId()), 460);
        p.add(left(id));
        p.add(Box.createVerticalStrut(14));

        JLabel memLabel = Ui.label("Память для игры: " + whole("memoryGb", 3) + " ГБ", 12, Ui.MUTED, Font.PLAIN);
        p.add(left(memLabel));
        JSlider mem = new JSlider(2, 16, whole("memoryGb", 3));
        mem.setOpaque(false);
        mem.setMaximumSize(new Dimension(460, 30));
        mem.addChangeListener(e -> memLabel.setText("Память для игры: " + mem.getValue() + " ГБ"));
        p.add(left(mem));
        p.add(Box.createVerticalStrut(10));

        p.add(left(Ui.label("Дополнительные аргументы Java", 12, Ui.MUTED, Font.PLAIN)));
        p.add(Box.createVerticalStrut(6));
        JTextField jvm = sized(Ui.field(text("jvmArgs", "")), 460);
        p.add(left(jvm));
        p.add(Box.createVerticalStrut(12));

        JCheckBox minimize = new JCheckBox("Сворачивать лаунчер на время игры", flag("minimize", true));
        minimize.setOpaque(false);
        minimize.setForeground(Ui.TEXT);
        minimize.setFont(Ui.font(Font.PLAIN, 13));
        p.add(left(minimize));
        p.add(Box.createVerticalStrut(14));

        p.add(left(Ui.label("Цвет лаунчера", 12, Ui.MUTED, Font.PLAIN)));
        p.add(Box.createVerticalStrut(6));
        JPanel chips = new JPanel();
        chips.setOpaque(false);
        chips.setLayout(new BoxLayout(chips, BoxLayout.X_AXIS));
        for (Color c : ACCENTS) {
            chips.add(new JComponent() {
                {
                    setPreferredSize(new Dimension(30, 30));
                    setMaximumSize(new Dimension(30, 30));
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            Ui.accent = c;
                            settings.put("accent", String.format("#%06X", c.getRGB() & 0xFFFFFF));
                            save();
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D)g0.create();
                    Ui.aa(g);
                    g.setColor(c);
                    g.fillOval(3, 3, 24, 24);
                    if (c.equals(Ui.accent)) {
                        g.setColor(Color.WHITE);
                        g.drawOval(1, 1, 28, 28);
                    }
                    g.dispose();
                }
            });
            chips.add(Box.createHorizontalStrut(6));
        }
        chips.setMaximumSize(new Dimension(460, 34));
        chips.setPreferredSize(new Dimension(460, 34));
        p.add(left(chips));
        p.add(Box.createVerticalStrut(18));

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(new Ui.Btn("Сохранить", true).onClick(() -> {
            String before = clientId();
            settings.put("msClientId", id.getText().trim());
            settings.put("memoryGb", (double)mem.getValue());
            settings.put("jvmArgs", jvm.getText().trim());
            settings.put("minimize", minimize.isSelected());
            if (!before.equals(clientId())) {
                settings.remove("accounts");
                settings.remove("selected");
                refreshAccounts();
            }
            save();
            status.setText("Настройки сохранены.");
        }));
        row.add(Box.createHorizontalStrut(12));
        row.add(new Ui.Btn("Папка игры", false).onClick(() -> {
            try {
                Files.createDirectories(Game.HOME.resolve("game"));
                Desktop.getDesktop().open(Game.HOME.resolve("game").toFile());
            } catch (Exception ignored) {
                // Nothing to open.
            }
        }));
        row.add(Box.createHorizontalGlue());
        row.setMaximumSize(new Dimension(460, 54));
        p.add(left(row));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JTextField sized(JTextField f, int width) {
        f.setMaximumSize(new Dimension(width, 38));
        f.setPreferredSize(new Dimension(width, 38));
        return f;
    }

    // ---- accounts ----

    private void refreshAccounts() {
        accountList.removeAll();
        List<Map<String, Object>> list = accounts();
        int chosen = Math.max(0, Math.min(whole("selected", 0), list.size() - 1));
        if (list.isEmpty()) {
            accountList.add(left(Ui.label("Аккаунтов пока нет.", 14, Ui.MUTED, Font.PLAIN)));
            accountLabel.setText("Не выполнен вход: открой вкладку «Аккаунты»");
            accountLabel.setForeground(Ui.MUTED);
        }
        for (int i = 0; i < list.size(); i++) {
            int index = i;
            Map<String, Object> acc = list.get(i);
            Ui.Card row = new Ui.Card();
            row.setLayout(new BorderLayout());
            row.setBorder(new EmptyBorder(10, 16, 10, 12));
            row.setMaximumSize(new Dimension(560, 58));
            row.setPreferredSize(new Dimension(560, 58));
            JLabel name = Ui.label(Json.str(acc.get("name")) + (i == chosen ? "   · выбран" : ""), 15, i == chosen ? Ui.TEXT : Ui.MUTED, Font.BOLD);
            row.add(name, BorderLayout.WEST);
            JPanel buttons = new JPanel();
            buttons.setOpaque(false);
            Ui.Btn pick = new Ui.Btn("Выбрать", false).onClick(() -> {
                settings.put("selected", (double)index);
                save();
                refreshAccounts();
            });
            pick.setPreferredSize(new Dimension(100, 34));
            Ui.Btn remove = new Ui.Btn("Удалить", false).onClick(() -> {
                List<Map<String, Object>> now = accounts();
                now.remove(index);
                settings.put("selected", 0.0);
                storeAccounts(now);
                refreshAccounts();
            });
            remove.setPreferredSize(new Dimension(100, 34));
            buttons.add(pick);
            buttons.add(remove);
            row.add(buttons, BorderLayout.EAST);
            left(row);
            accountList.add(row);
            accountList.add(Box.createVerticalStrut(8));
            if (i == chosen) {
                accountLabel.setText("Аккаунт: " + Json.str(acc.get("name")));
                accountLabel.setForeground(Ui.TEXT);
            }
        }
        accountList.revalidate();
        accountList.repaint();
    }

    private boolean needClientId() {
        if (!clientId().isEmpty()) {
            return false;
        }
        JOptionPane.showMessageDialog(frame,
            "Для входа через Microsoft нужен Client ID приложения.\nВведи его во вкладке «Настройки» (как получить — в README лаунчера).",
            "DooreVisuals", JOptionPane.INFORMATION_MESSAGE);
        tab(4);
        return true;
    }

    private void addAccount() {
        if (busy || needClientId()) {
            return;
        }
        busy = true;
        accountStatus.setForeground(Ui.MUTED);
        accountStatus.setText("Вход через Microsoft…");
        new Thread(() -> {
            try {
                Auth.Session s = Auth.signIn(clientId(), codeAndUrl -> {
                    try {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(codeAndUrl[0]), null);
                        Desktop.getDesktop().browse(URI.create(codeAndUrl[1]));
                    } catch (Exception ignored) {
                        // The code is shown anyway.
                    }
                    SwingUtilities.invokeLater(() -> accountStatus.setText(
                        "Введи код " + codeAndUrl[0] + " на странице " + codeAndUrl[1] + " (код уже в буфере обмена)"));
                });
                SwingUtilities.invokeLater(() -> {
                    remember(s);
                    accountStatus.setForeground(Ui.OK);
                    accountStatus.setText("Добавлен аккаунт " + s.name());
                    refreshAccounts();
                    busy = false;
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    accountStatus.setForeground(Ui.BAD);
                    accountStatus.setText(String.valueOf(e.getMessage()));
                    busy = false;
                });
            }
        }, "sign-in").start();
    }

    /** Stores a session's refresh token: a new account is added and chosen, a known one is updated. */
    private void remember(Auth.Session s) {
        List<Map<String, Object>> list = accounts();
        int at = -1;
        for (int i = 0; i < list.size(); i++) {
            if (Json.str(list.get(i).get("uuid")).equals(s.uuid())) {
                at = i;
            }
        }
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("name", s.name());
        acc.put("uuid", s.uuid());
        acc.put("refreshToken", s.refreshToken());
        if (at < 0) {
            list.add(acc);
            at = list.size() - 1;
            settings.put("selected", (double)at);
        } else {
            list.set(at, acc);
        }
        storeAccounts(list);
    }

    // ---- play ----

    private void onPlay() {
        if (busy) {
            return;
        }
        if (needClientId()) {
            return;
        }
        Map<String, Object> acc = selectedAccount();
        if (acc == null) {
            status.setForeground(Ui.BAD);
            status.setText("Сначала добавь аккаунт Microsoft.");
            tab(1);
            return;
        }
        settings.put("server", serverField.getText().trim());
        save();
        busy = true;
        playBtn.setEnabled(false);
        status.setForeground(Ui.MUTED);
        new Thread(() -> run(acc), "play").start();
    }

    private void say(String text, boolean bad) {
        SwingUtilities.invokeLater(() -> {
            status.setForeground(bad ? Ui.BAD : Ui.MUTED);
            status.setText(text);
        });
    }

    private void run(Map<String, Object> acc) {
        Game.Progress progress = new Game.Progress() {
            @Override
            public void status(String text) {
                say(text, false);
            }

            @Override
            public void fraction(double value) {
                SwingUtilities.invokeLater(() -> bar.set(value));
            }
        };
        boolean minimized = false;
        try {
            Auth.Session session;
            try {
                session = Auth.refresh(clientId(), Json.str(acc.get("refreshToken")));
            } catch (IOException e) {
                throw new IOException("Нужно войти заново (вкладка «Аккаунты»): " + e.getMessage());
            }
            SwingUtilities.invokeLater(() -> remember(session));
            Game.Plan plan = Game.install(progress);
            Mods.sync(appDir, progress);
            progress.status("Запуск игры…");
            Process process = Game.start(plan, session, whole("memoryGb", 3), clientId(), text("jvmArgs", ""), text("server", ""));
            SwingUtilities.invokeLater(() -> {
                bar.set(1.0);
                console.setText("");
                if (flag("minimize", true)) {
                    frame.setState(Frame.ICONIFIED);
                }
            });
            minimized = flag("minimize", true);
            say("Игра запущена", false);
            pump(process);
            int code = process.waitFor();
            say(code == 0 ? "Игра закрыта." : "Игра завершилась с ошибкой (код " + code + "), подробности во вкладке «Консоль».", code != 0);
        } catch (Exception e) {
            say(String.valueOf(e.getMessage()), true);
        } finally {
            boolean restore = minimized;
            SwingUtilities.invokeLater(() -> {
                busy = false;
                playBtn.setEnabled(true);
                bar.set(0);
                if (restore) {
                    frame.setState(Frame.NORMAL);
                    frame.toFront();
                }
            });
        }
    }

    /** Reads the game's output into the console tab and into launcher-game.log until the game ends. */
    private void pump(Process process) {
        Path log = Game.HOME.resolve("launcher-game.log");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            Files.writeString(log, "");
            String line;
            while ((line = in.readLine()) != null) {
                String l = line;
                Files.writeString(log, l + System.lineSeparator(), StandardOpenOption.APPEND);
                SwingUtilities.invokeLater(() -> {
                    console.append(l + "\n");
                    if (console.getDocument().getLength() > 400_000) {
                        try {
                            console.getDocument().remove(0, 100_000);
                        } catch (Exception ignored) {
                            // Keep the text as it is.
                        }
                    }
                    console.setCaretPosition(console.getDocument().getLength());
                });
            }
        } catch (IOException ignored) {
            // The process ended.
        }
    }

    // ---- news ----

    private void loadNews() {
        try {
            Path bundled = appDir.resolve("news.txt");
            news.setText(Files.isRegularFile(bundled) ? Files.readString(bundled) : "Новостей пока нет.");
        } catch (IOException e) {
            news.setText("Новостей пока нет.");
        }
        new Thread(() -> {
            try {
                Map<String, Object> release = Json.obj(Json.parse(Http.text("https://api.github.com/repos/zxchores/DooreVisuals/releases/latest")));
                String body = Json.str(release.get("body")).trim();
                if (!body.isEmpty()) {
                    String text = Json.str(release.get("name")) + "\n\n" + body;
                    SwingUtilities.invokeLater(() -> {
                        news.setText(text);
                        news.setCaretPosition(0);
                    });
                }
            } catch (Exception ignored) {
                // The bundled news stay.
            }
        }, "news").start();
    }
}
