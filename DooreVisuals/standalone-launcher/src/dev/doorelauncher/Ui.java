package dev.doorelauncher;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/** The look of the launcher: colours, an animated background, and the few custom controls it needs. */
final class Ui {
    static final Color BG1 = new Color(0x0C0A12);
    static final Color BG2 = new Color(0x1A1430);
    static final Color PANEL = new Color(255, 255, 255, 14);
    static final Color PANEL_HOT = new Color(255, 255, 255, 30);
    static final Color TEXT = new Color(0xEEEBF8);
    static final Color MUTED = new Color(0x9A94B8);
    static final Color OK = new Color(0x7BD88F);
    static final Color BAD = new Color(0xE87B8B);

    /** The accent colour of the launcher; changed from the settings. */
    static Color accent = new Color(0x8B7BD8);

    private static final List<Runnable> TICKS = new ArrayList<>();

    private Ui() {
    }

    static void ticker(Runnable r) {
        TICKS.add(r);
    }

    static void startClock(JComponent repaint) {
        new Timer(16, e -> {
            for (Runnable r : TICKS) {
                r.run();
            }
            repaint.repaint();
        }).start();
    }

    static Font font(int style, float size) {
        return new Font("Segoe UI", style, 1).deriveFont(style, size);
    }

    static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float t) {
        return new Color((int)(a.getRed() + (b.getRed() - a.getRed()) * t), (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static JLabel label(String text, float size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    static JTextField field(String text) {
        JTextField f = new JTextField(text);
        f.setFont(font(Font.PLAIN, 14));
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBackground(new Color(0x1E1830));
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 30)), new EmptyBorder(8, 10, 8, 10)));
        return f;
    }

    /** The window background: a slow gradient with drifting soft lights. */
    static final class Backdrop extends JPanel {
        private final Random random = new Random(11);
        private final float[][] lights = new float[26][5];
        private float phase;

        Backdrop() {
            setOpaque(true);
            for (float[] l : lights) {
                reset(l, true);
            }
            ticker(() -> {
                phase += 0.004F;
                for (float[] l : lights) {
                    l[0] += l[2];
                    l[1] += l[3];
                    if (l[1] < -0.1F || l[0] < -0.1F || l[0] > 1.1F) {
                        reset(l, false);
                    }
                }
            });
        }

        private void reset(float[] l, boolean anywhere) {
            l[0] = random.nextFloat();
            l[1] = anywhere ? random.nextFloat() : 1.08F;
            l[2] = (random.nextFloat() - 0.5F) * 0.0006F;
            l[3] = -(0.0005F + random.nextFloat() * 0.0012F);
            l[4] = 0.4F + random.nextFloat();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D)g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            Color tint = mix(BG2, accent, 0.18F);
            float wob = (float)Math.sin(phase) * 0.5F + 0.5F;
            g.setPaint(new GradientPaint(0, 0, BG1, w * (0.6F + 0.4F * wob), h, tint));
            g.fillRect(0, 0, w, h);
            for (float[] l : lights) {
                int r = (int)(l[4] * 46);
                g.setColor(alpha(accent, (int)(26 * l[4])));
                g.fillOval((int)(l[0] * w) - r, (int)(l[1] * h) - r, r * 2, r * 2);
            }
            g.dispose();
        }
    }

    /** A rounded button that glows towards the accent colour under the pointer. */
    static final class Btn extends JComponent {
        private final String text;
        private final boolean primary;
        private float hover;
        private boolean over;
        private Runnable action = () -> { };

        Btn(String text, boolean primary) {
            this.text = text;
            this.primary = primary;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(primary ? 240 : 150, primary ? 52 : 38));
            setMaximumSize(getPreferredSize());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over = false;
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isEnabled()) {
                        action.run();
                    }
                }
            });
            ticker(() -> hover += ((over && isEnabled() ? 1.0F : 0.0F) - hover) * 0.22F);
        }

        Btn onClick(Runnable r) {
            this.action = r;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D)g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            int arc = primary ? 18 : 12;
            if (primary) {
                Color a = mix(accent, Color.WHITE, 0.10F * hover);
                Color b = mix(accent, new Color(0x4D3FA8), 0.55F - 0.25F * hover);
                g.setPaint(new GradientPaint(0, 0, a, w, h, b));
                if (!isEnabled()) {
                    g.setPaint(new GradientPaint(0, 0, alpha(a, 90), w, h, alpha(b, 90)));
                }
                g.fillRoundRect(0, 0, w, h, arc, arc);
                g.setColor(alpha(Color.WHITE, (int)(30 + 40 * hover)));
                g.setStroke(new BasicStroke(1.0F));
                g.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                g.setColor(mix(new Color(0x2F2850), accent, 0.35F * hover));
                g.fillRoundRect(0, 0, w, h, arc, arc);
            }
            g.setFont(font(Font.BOLD, primary ? 17 : 13));
            g.setColor(isEnabled() ? TEXT : MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** A side-menu entry: a label with an accent bar that slides in when it is the chosen one. */
    static final class Nav extends JComponent {
        private final String text;
        private float hover;
        private float chosen;
        private boolean over;
        private boolean selected;

        Nav(String text, Runnable action) {
            this.text = text;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(190, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    over = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    over = false;
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    action.run();
                }
            });
            ticker(() -> {
                hover += ((over ? 1.0F : 0.0F) - hover) * 0.2F;
                chosen += ((selected ? 1.0F : 0.0F) - chosen) * 0.18F;
            });
        }

        void select(boolean value) {
            selected = value;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D)g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(alpha(Color.WHITE, (int)(8 * hover + 14 * chosen)));
            g.fillRoundRect(10, 3, w - 20, h - 6, 12, 12);
            g.setColor(accent);
            int bar = (int)((h - 20) * chosen);
            g.fillRoundRect(10, (h - bar) / 2, 3, Math.max(0, bar), 3, 3);
            g.setFont(font(Font.BOLD, 14));
            g.setColor(mix(MUTED, TEXT, Math.max(hover * 0.6F, chosen)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 28 + (int)(4 * chosen), (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    /** A thin rounded progress bar. */
    static final class Bar extends JComponent {
        private float value;
        private float shown;

        Bar() {
            setPreferredSize(new Dimension(240, 8));
            ticker(() -> shown += (value - shown) * 0.15F);
        }

        void set(double v) {
            value = (float)Math.max(0.0, Math.min(1.0, v));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D)g0.create();
            aa(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(alpha(Color.WHITE, 22));
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setPaint(new GradientPaint(0, 0, accent, w, 0, mix(accent, Color.WHITE, 0.35F)));
            g.fillRoundRect(0, 0, Math.max(h, (int)(w * shown)), h, h, h);
            g.dispose();
        }
    }

    /** A translucent rounded card that holds a block of content. */
    static final class Card extends JPanel {
        Card() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D)g0.create();
            aa(g);
            g.setColor(PANEL);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.setColor(alpha(Color.WHITE, 16));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.dispose();
        }
    }
}
