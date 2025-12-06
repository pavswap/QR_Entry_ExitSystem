import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QRProject4 {

    // Time Tracking Implementation: Using a Map to store QR data and its entry time.
    private static final Map<String, LocalDateTime> entryMap = new HashMap<>();

    private static DefaultListModel<String> listModel;
    private static DefaultListModel<String> historyModel;
    private static JLabel statusLabel;
    private static JLabel countLabel;
    private static JScrollPane currentScroll;
    private static JScrollPane historyScroll;

    // --- Enhanced Modern Color Palette and Fonts (Slightly Softer) ---
    private static final Color DARK_BLUE = new Color(25, 25, 50); // Softer base dark blue
    private static final Color MIDNIGHT_BLUE = new Color(45, 45, 80); // Softer gradient blue
    private static final Color VIOLET_ACCENT = new Color(173, 58, 255); // Brighter, more electric Violet
    private static final Color SUCCESS_GREEN = new Color(0, 255, 128); // Neon Green for high contrast
    private static final Color EXIT_RED = new Color(255, 80, 80); // Clear, vibrant red
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color LIST_BG = new Color(35, 35, 65); // Darker list background
    private static final Color METRICS_BG = new Color(55, 55, 95); // Darker, distinct metrics background

    private static final Font INTER_BOLD_36 = new Font("Inter", Font.BOLD, 36);
    private static final Font INTER_BOLD_24 = new Font("Inter", Font.BOLD, 24); // Larger for impact
    private static final Font INTER_BOLD_18 = new Font("Inter", Font.BOLD, 18);
    private static final Font INTER_PLAIN_16 = new Font("Inter", Font.PLAIN, 16);
    private static final Font INTER_PLAIN_14 = new Font("Inter", Font.PLAIN, 14);

    private static final String LOG_DIR = "logs";

    // --- NEW: Custom Rounded Panel Class ---
    static class RoundedPanel extends JPanel {
        private int cornerRadius = 30; // Increased radius for a more bubbly look
        private Color shadowColor = new Color(0, 0, 0, 100);

        public RoundedPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false); // Must be false for the rounded corners to show
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // 1. Draw Subtle Shadow
            g2.setColor(shadowColor);
            g2.fillRoundRect(3, 3, width - 6, height - 6, cornerRadius, cornerRadius);

            // 2. Draw Main Panel Background
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // --- NEW: Custom Rounded Titled Border Class ---
    static class RoundedBorder implements Border {
        private int radius;
        private Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 5, radius + 5, radius + 5, radius + 5);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            // Draw a slightly thicker line for the outline
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        // Apply a cleaner Look and Feel if available
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Fallback to default if Nimbus is not available
        }

        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.out.println("❌ No webcam detected!");
            return;
        }
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        // MainFrame
        JFrame frame = new JFrame("💾 Secure Entry/Exit System (QR)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 850);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);

        // BackgroundPanel (Main Content) - Uses the updated diagonal gradient
        GradientPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(30, 30));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25)); // Overall padding
        frame.setContentPane(mainPanel);

        // Header Label
        JLabel title = new JLabel("SECURE ACCESS CONTROL", SwingConstants.CENTER);
        title.setFont(INTER_BOLD_36);
        title.setForeground(TEXT_WHITE);
        mainPanel.add(title, BorderLayout.NORTH);

        // --- Center Panel for Webcam and Status ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Webcam Setup: WRAPPED IN RoundedPanel
        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(false); // Cleaned up display
        webcamPanel.setMirrored(true);
        webcamPanel.setPreferredSize(new Dimension(650, 650));
        webcamPanel.setOpaque(false); // Ensure webcam panel doesn't draw a background
        webcamPanel.setBorder(null); // Remove original border

        // Add WebcamPanel to a RoundedPanel container
        RoundedPanel webcamContainer = new RoundedPanel(new BorderLayout());
        webcamContainer.setBackground(MIDNIGHT_BLUE.darker()); // Set background for the rounded area
        webcamContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Inner padding
        webcamContainer.add(webcamPanel, BorderLayout.CENTER);

        // Thicker, glowing border for a high-tech feel
        webcamContainer.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(30, VIOLET_ACCENT), // Outer rounded line border
                BorderFactory.createEmptyBorder(5, 5, 5, 5) // Inner spacing
        ));


        centerPanel.add(webcamContainer, BorderLayout.CENTER);

        // Status Label (for real-time feedback)
        statusLabel = new JLabel("System Initialized. Ready to Scan...", SwingConstants.CENTER);
        statusLabel.setFont(INTER_BOLD_24);
        statusLabel.setForeground(TEXT_WHITE);
        statusLabel.setPreferredSize(new Dimension(650, 40));
        centerPanel.add(statusLabel, BorderLayout.SOUTH);

        // --- Right Panel (Side Panel) for Metrics, Lists, and Buttons ---
        JPanel sidePanel = new JPanel(new BorderLayout(15, 15));
        sidePanel.setPreferredSize(new Dimension(450, 700));
        sidePanel.setOpaque(false);
        mainPanel.add(sidePanel, BorderLayout.EAST);

        // 1. Metrics Panel (Now uses RoundedPanel)
        JPanel metricsPanel = createMetricsPanel();
        sidePanel.add(metricsPanel, BorderLayout.NORTH);

        // 2. Lists Panel

        // Custom Rounded Border Style for Lists
        int listRadius = 20;
        Color listBorderColor = VIOLET_ACCENT.darker().darker();
        Border roundedLine = new RoundedBorder(listRadius, listBorderColor);
        Border emptyPadding = new EmptyBorder(5, 5, 5, 5); // Add padding inside the border

        // CurrentlyInside_List Border
        TitledBorder currentInsideBorder = BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(roundedLine, emptyPadding), // Combine line and padding
                "CURRENTLY INSIDE", // ALL CAPS for modern UI
                TitledBorder.CENTER,
                TitledBorder.TOP,
                INTER_BOLD_18,
                VIOLET_ACCENT.brighter());
        currentInsideBorder.setTitleColor(VIOLET_ACCENT.brighter());
        currentInsideBorder.setTitleJustification(TitledBorder.CENTER);
        currentInsideBorder.setTitlePosition(TitledBorder.ABOVE_TOP);

        // DailyLogs Border
        TitledBorder dailyLogBorder = BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(roundedLine, emptyPadding),
                "TODAY'S SCAN LOG", // ALL CAPS for modern UI
                TitledBorder.CENTER,
                TitledBorder.TOP,
                INTER_BOLD_18,
                VIOLET_ACCENT.brighter());
        dailyLogBorder.setTitleColor(VIOLET_ACCENT.brighter());
        dailyLogBorder.setTitleJustification(TitledBorder.CENTER);
        dailyLogBorder.setTitlePosition(TitledBorder.ABOVE_TOP);

        // CurrentlyInside_List
        listModel = new DefaultListModel<>();
        JList<String> entryList = new JList<>(listModel);
        styleList(entryList);
        currentScroll = new JScrollPane(entryList);
        currentScroll.setBorder(currentInsideBorder); // Apply new border
        currentScroll.getViewport().setBackground(LIST_BG); // Ensure viewport matches list background

        // DailyLogs
        historyModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyModel);
        historyList.setCellRenderer(new LogCellRenderer()); // Use custom renderer
        styleList(historyList);
        historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(dailyLogBorder); // Apply new border
        historyScroll.getViewport().setBackground(LIST_BG); // Ensure viewport matches list background


        // Split Panel for Lists
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currentScroll, historyScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOpaque(false);
        splitPane.setDividerSize(10);
        splitPane.setBorder(null);
        sidePanel.add(splitPane, BorderLayout.CENTER);

        // 3. Refresh Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        // Use an urgent/danger color for the "Clear" action
        JButton refreshButton = new JButton("🚨 CLEAR ALL DAILY DATA");
        styleButton(refreshButton, EXIT_RED.darker()); // Pass the base color
        buttonPanel.add(refreshButton);
        sidePanel.add(buttonPanel, BorderLayout.SOUTH);
        refreshButton.addActionListener(e -> clearLogs());

        frame.setVisible(true);

        // Load logs and update initial count
        loadTodayLog();
        updateCountLabel();

        // ScanningThread remains the same (1.5 second debounce)
        Thread scannerThread = new Thread(() -> {
            while (true) {
                try {
                    if (!webcam.isOpen()) break;
                    BufferedImage image = webcam.getImage();
                    if (image == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    Result result = null;
                    try {
                        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                        result = new MultiFormatReader().decode(bitmap, hints);
                    } catch (NotFoundException ignored) {}

                    if (result != null) {
                        String qrData = result.getText().trim();
                        SwingUtilities.invokeLater(() -> updateLists(qrData));
                        // 1.5 second delay as requested
                        Thread.sleep(1500);
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error during scanning: " + e.getMessage());
                }
            }
        });

        scannerThread.setDaemon(true);
        scannerThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(webcam::close));
    }

    // --- UI Component Builders ---

    private static JPanel createMetricsPanel() {
        // CHANGED to use RoundedPanel
        RoundedPanel panel = new RoundedPanel(new BorderLayout());
        panel.setBackground(METRICS_BG);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20)); // Padding inside the rounded panel

        countLabel = new JLabel("Inside: 0", SwingConstants.LEFT);
        countLabel.setFont(INTER_BOLD_24);
        countLabel.setForeground(SUCCESS_GREEN);

        JLabel labelTitle = new JLabel("LIVE STATUS METRICS", SwingConstants.RIGHT);
        labelTitle.setFont(INTER_PLAIN_16);
        labelTitle.setForeground(TEXT_WHITE.darker());

        panel.add(countLabel, BorderLayout.WEST);
        panel.add(labelTitle, BorderLayout.EAST);
        panel.setPreferredSize(new Dimension(450, 80)); // Taller for impact

        return panel;
    }

    private static void updateCountLabel() {
        countLabel.setText("Inside: " + entryMap.size());
        countLabel.setForeground(entryMap.size() > 0 ? SUCCESS_GREEN : VIOLET_ACCENT.brighter());
        countLabel.setFont(INTER_BOLD_24); // Ensure font size is consistent

        // Border updates for count
        TitledBorder border = (TitledBorder) currentScroll.getBorder();
        border.setTitle("CURRENTLY INSIDE (" + entryMap.size() + ")");
        currentScroll.repaint();
    }

    // --- UI/Utility Methods ---

    // Background Gradient Panel (Diagonal Gradient - Softer Colors)
    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Color color1 = DARK_BLUE;
            Color color2 = MIDNIGHT_BLUE.darker();
            // Diagonal Gradient for a modern look (from top-left to bottom-right)
            GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
        }
    }

    // Custom Renderer for Log History
    static class LogCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value != null) {
                String entry = (String) value;
                if (entry.startsWith("ENTRY")) {
                    setForeground(SUCCESS_GREEN);
                    setText("▶ " + entry);
                } else if (entry.startsWith("EXIT")) {
                    setForeground(EXIT_RED);
                    setText("◀ " + entry);
                } else {
                    setForeground(TEXT_WHITE);
                }
            }
            setBackground(LIST_BG);
            setFont(INTER_PLAIN_16);
            if (isSelected) {
                setBackground(VIOLET_ACCENT.darker());
            }
            setBorder(new EmptyBorder(8, 10, 8, 10)); // Increased vertical padding
            return this;
        }
    }

    // List styling
    private static void styleList(JList<String> list) {
        list.setBackground(LIST_BG);
        list.setForeground(TEXT_WHITE);
        list.setFont(INTER_PLAIN_16);
        list.setSelectionBackground(VIOLET_ACCENT.brighter().brighter()); // More vibrant selection
        list.setSelectionForeground(Color.BLACK);
    }

    // Buttons (takes base color for dynamic styling)
    private static void styleButton(JButton button, Color baseColor) {
        button.setFont(INTER_BOLD_18);
        button.setForeground(TEXT_WHITE);
        button.setBackground(baseColor);
        button.setOpaque(false); // Crucial for custom shape
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(300, 50));

        // Use custom UI to draw a rounded, pill-like button
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                JButton btn = (JButton) c;
                int w = btn.getWidth();
                int h = btn.getHeight();
                int arc = 25; // Large radius for pill shape

                Color currentBg = btn.getModel().isRollover() ? baseColor.brighter() : baseColor;

                // Fill background
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Draw border
                g2.setColor(baseColor.brighter());
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // Paint text
                super.paint(g, c);
                g2.dispose();
            }
        });
    }

    // Utility function to format Duration
    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%d h %02d m %02d s",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    // UpdateList Logic
    private static void updateLists(String qrData) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry;

        if (entryMap.containsKey(qrData)) {
            // --- EXIT LOGIC (Calculate Time Spent) ---
            LocalDateTime entryTime = entryMap.get(qrData);
            Duration duration = Duration.between(entryTime, now);
            String timeSpent = formatDuration(duration);

            entryMap.remove(qrData);

            // Find and remove the display element from the listModel
            int index = -1;
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.getElementAt(i).startsWith(qrData + " ")) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                listModel.remove(index);
            }

            logEntry = "EXIT  | " + qrData + " | " + timestamp + " | Spent: " + timeSpent;
            statusLabel.setText("🚪 EXIT: " + qrData + " (Duration: " + timeSpent + ")");
            statusLabel.setForeground(EXIT_RED);
            System.out.println("🚪 EXIT: " + qrData + " | Time Spent: " + timeSpent);
        } else {
            // --- ENTRY LOGIC ---
            entryMap.put(qrData, now);
            String displayTime = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // Add a descriptive element to the display list
            listModel.addElement(qrData + " (Entry: " + displayTime + ")");

            logEntry = "ENTRY | " + qrData + " | " + timestamp;
            statusLabel.setText("✅ ENTRY: " + qrData);
            statusLabel.setForeground(SUCCESS_GREEN);
            System.out.println("✅ ENTRY: " + qrData);
        }

        updateCountLabel();

        historyModel.addElement(logEntry);
        // Scroll to the latest log entry
        SwingUtilities.invokeLater(() -> historyScroll.getVerticalScrollBar().setValue(historyScroll.getVerticalScrollBar().getMaximum()));

        appendToLogFile(logEntry);
    }

    // FileHandling
    private static Path getTodayLogFile() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException ignored) {}
        String fileName = LocalDate.now().toString() + "_log.txt";
        return Paths.get(LOG_DIR, fileName);
    }

    private static void appendToLogFile(String logEntry) {
        try (FileWriter fw = new FileWriter(getTodayLogFile().toFile(), true)) {
            fw.write(logEntry + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadTodayLog() {
        Path file = getTodayLogFile();
        if (Files.exists(file)) {
            try {
                // Temporarily store who is currently inside to calculate their entry time
                Map<String, LocalDateTime> currentEntries = new HashMap<>();

                for (String line : Files.readAllLines(file)) {
                    String[] parts = line.split(" \\| ");

                    if (parts.length >= 2) {
                        String action = parts[0].trim();
                        String qrData = parts[1].trim();

                        // When loading from file, we don't have the real entry time from the past,
                        // so we use the time now as a placeholder for duration calculation in the current session.
                        // This allows for correct time tracking for current entries if the app is restarted mid-day.
                        if (action.equals("ENTRY")) {
                            currentEntries.put(qrData, LocalDateTime.now());
                        } else if (action.startsWith("EXIT")) {
                            currentEntries.remove(qrData);
                        }
                    }
                    historyModel.addElement(line);
                }

                // Populate the global entryMap and the listModel with the recovered data
                entryMap.putAll(currentEntries);
                for(Map.Entry<String, LocalDateTime> entry : entryMap.entrySet()) {
                    String qrData = entry.getKey();
                    // We use the current time as the display time, as the original entry time from the file is lost.
                    String displayTime = entry.getValue().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    listModel.addElement(qrData + " (Entry: " + displayTime + ")");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void clearLogs() {
        // Clear runtime state
        entryMap.clear();
        listModel.clear();
        historyModel.clear();
        statusLabel.setText("Logs Cleared. Ready to Scan.");
        statusLabel.setForeground(TEXT_WHITE);

        // Clear file log
        Path file = getTodayLogFile();
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateCountLabel();

        // Simple visual confirmation
        JOptionPane.showMessageDialog(null, "✅ All entries and logs cleared for today!", "Logs Cleared", JOptionPane.INFORMATION_MESSAGE);
    }
}
