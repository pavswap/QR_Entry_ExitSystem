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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QRProject3 {

    private static final Set<String> entrySet = new HashSet<>();
    private static DefaultListModel<String> listModel;
    private static DefaultListModel<String> historyModel;
    private static JLabel statusLabel;
    private static JScrollPane currentScroll; // ADDED: Static reference to the JScrollPane
    private static final String LOG_DIR = "logs";

    // --- Modern Color Palette ---
    private static final Color DARK_BLUE = new Color(20, 20, 40);
    private static final Color MIDNIGHT_BLUE = new Color(30, 30, 60);
    private static final Color VIOLET_ACCENT = new Color(138, 43, 226); // Blue Violet
    private static final Color SUCCESS_GREEN = new Color(124, 252, 0); // Lime Green
    private static final Color EXIT_RED = new Color(255, 100, 100); // Soft Red
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color LIST_BG = new Color(45, 45, 75); // Slightly lighter than main background

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
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);

        // BackgroundPanel (Main Content)
        GradientPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(30, 30));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25)); // Overall padding
        frame.setContentPane(mainPanel);

        // Header Label
        JLabel title = new JLabel("SECURE QR SCANNER", SwingConstants.CENTER);
        title.setFont(new Font("Inter", Font.BOLD, 36));
        title.setForeground(TEXT_WHITE);
        mainPanel.add(title, BorderLayout.NORTH);

        // --- Center Panel for Webcam and Status ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Webcam Setup
        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setMirrored(true);
        webcamPanel.setPreferredSize(new Dimension(650, 650));
        webcamPanel.setBorder(BorderFactory.createLineBorder(VIOLET_ACCENT, 4, true)); // Accent border

        centerPanel.add(webcamPanel, BorderLayout.CENTER);

        // Status Label (for real-time feedback)
        statusLabel = new JLabel("Ready to Scan...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Inter", Font.BOLD, 24));
        statusLabel.setForeground(TEXT_WHITE);
        statusLabel.setPreferredSize(new Dimension(650, 40));
        centerPanel.add(statusLabel, BorderLayout.SOUTH);

        // --- Right Panel for Entries and Logs ---
        JPanel sidePanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setOpaque(false);
            }
        };
        sidePanel.setPreferredSize(new Dimension(450, 700));
        sidePanel.setOpaque(false);
        mainPanel.add(sidePanel, BorderLayout.EAST);

        // Custom Titled Border Style
        Border lineBorder = BorderFactory.createLineBorder(VIOLET_ACCENT, 2);
        TitledBorder currentInsideBorder = BorderFactory.createTitledBorder(
                lineBorder,
                "Currently Inside (" + entrySet.size() + ")",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Inter", Font.BOLD, 18),
                VIOLET_ACCENT);
        currentInsideBorder.setTitleColor(TEXT_WHITE);

        TitledBorder dailyLogBorder = BorderFactory.createTitledBorder(
                lineBorder,
                "Today's Scan Log",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Inter", Font.BOLD, 18),
                VIOLET_ACCENT);
        dailyLogBorder.setTitleColor(TEXT_WHITE);

        // CurrentlyInside_List
        listModel = new DefaultListModel<>();
        JList<String> entryList = new JList<>(listModel);
        styleList(entryList);
        currentScroll = new JScrollPane(entryList); // ASSIGNMENT to static variable
        currentScroll.setBorder(currentInsideBorder);

        // DailyLogs
        historyModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyModel);
        historyList.setCellRenderer(new LogCellRenderer()); // Use custom renderer
        styleList(historyList);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(dailyLogBorder);

        // Split Panel for Lists
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currentScroll, historyScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOpaque(false);
        splitPane.setDividerSize(10);
        splitPane.setBorder(null);
        sidePanel.add(splitPane, BorderLayout.CENTER);

        // RefreshButton Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setOpaque(false);
            }
        };
        JButton refreshButton = new JButton("⟳ Refresh / Clear Logs");
        styleButton(refreshButton);
        buttonPanel.add(refreshButton);
        sidePanel.add(buttonPanel, BorderLayout.SOUTH);
        refreshButton.addActionListener(e -> clearLogs());

        frame.setVisible(true);

        // Load logs
        loadTodayLog();

        // ScanningThread
        Thread scannerThread = new Thread(() -> {
            while (true) {
                try {
                    // Check if the webcam is still available before attempting to get the image
                    if (!webcam.isOpen()) {
                        break; // Exit loop if webcam is closed
                    }
                    BufferedImage image = webcam.getImage();
                    if (image == null) {
                        Thread.sleep(100);
                        continue;
                    }

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    Result result = null;
                    try {
                        // Use Hints for better decoding performance
                        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                        result = new MultiFormatReader().decode(bitmap, hints);
                    } catch (NotFoundException ignored) {
                        // QR code not found in the current frame
                    }

                    if (result != null) {
                        String qrData = result.getText().trim();
                        SwingUtilities.invokeLater(() -> updateLists(qrData));
                        Thread.sleep(2500); // Delay after successful scan to prevent rapid re-scans
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error during scanning: " + e.getMessage());
                    // Continue loop even if an error occurs
                }
            }
        });

        scannerThread.setDaemon(true);
        scannerThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(webcam::close));
    }

    // --- UI/Utility Methods ---

    // Background Gradient Panel
    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int width = getWidth();
            int height = getHeight();
            // Deep, sophisticated gradient
            Color color1 = DARK_BLUE;
            Color color2 = MIDNIGHT_BLUE;
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
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
            if (isSelected) {
                setBackground(VIOLET_ACCENT.darker());
            }
            setBorder(new EmptyBorder(5, 10, 5, 10)); // Add padding to list items
            return this;
        }
    }

    // List styling
    private static void styleList(JList<String> list) {
        list.setBackground(LIST_BG);
        list.setForeground(TEXT_WHITE);
        list.setFont(new Font("Consolas", Font.PLAIN, 16));
        list.setSelectionBackground(VIOLET_ACCENT.brighter());
        list.setSelectionForeground(Color.BLACK);
    }

    // Buttons
    private static void styleButton(JButton button) {
        button.setFont(new Font("Inter", Font.BOLD, 18));
        button.setForeground(TEXT_WHITE);
        button.setBackground(VIOLET_ACCENT);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createLineBorder(VIOLET_ACCENT.brighter(), 2));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(300, 50));

        // Add Hover Effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(VIOLET_ACCENT.brighter());
                button.setBorder(BorderFactory.createLineBorder(TEXT_WHITE, 2));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(VIOLET_ACCENT);
                button.setBorder(BorderFactory.createLineBorder(VIOLET_ACCENT.brighter(), 2));
            }
        });
    }

    // UpdateList Logic
    private static void updateLists(String qrData) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry;

        if (entrySet.contains(qrData)) {
            entrySet.remove(qrData);
            listModel.removeElement(qrData);
            logEntry = "EXIT  | " + qrData + " | " + timestamp;
            statusLabel.setText("🚪 EXIT SUCCESS: " + qrData);
            statusLabel.setForeground(EXIT_RED);
            System.out.println("🚪 EXIT: " + qrData);
        } else {
            entrySet.add(qrData);
            listModel.addElement(qrData);
            logEntry = "ENTRY | " + qrData + " | " + timestamp;
            statusLabel.setText("✅ ENTRY SUCCESS: " + qrData);
            statusLabel.setForeground(SUCCESS_GREEN);
            System.out.println("✅ ENTRY: " + qrData);
        }

        // Fix: Use the static currentScroll variable instead of listModel.getOwner()
        TitledBorder border = (TitledBorder) currentScroll.getBorder();
        border.setTitle("Currently Inside (" + entrySet.size() + ")");
        currentScroll.repaint();

        historyModel.addElement(logEntry);
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
                for (String line : Files.readAllLines(file)) {
                    // Extract QR data from log line to rebuild the entrySet
                    String[] parts = line.split(" \\| ");
                    if (parts.length >= 2) {
                        String action = parts[0].trim();
                        String qrData = parts[1].trim();

                        if (action.equals("ENTRY")) {
                            entrySet.add(qrData);
                        } else if (action.equals("EXIT")) {
                            entrySet.remove(qrData);
                        }
                    }
                    historyModel.addElement(line);
                }
                // Finally, populate the 'Currently Inside' list based on the reconstructed set
                for(String data : entrySet) {
                    listModel.addElement(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void clearLogs() {
        // Clear runtime state
        entrySet.clear();
        listModel.clear();
        historyModel.clear();
        statusLabel.setText("Logs Cleared.");
        statusLabel.setForeground(TEXT_WHITE);

        // Clear file log
        Path file = getTodayLogFile();
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fix: Use the static currentScroll variable instead of listModel.getOwner()
        TitledBorder border = (TitledBorder) currentScroll.getBorder();
        border.setTitle("Currently Inside (0)");
        currentScroll.repaint();

        // Simple visual confirmation
        JOptionPane.showMessageDialog(null, "✅ All entries and logs cleared for today!", "Logs Cleared", JOptionPane.INFORMATION_MESSAGE);
    }
}
