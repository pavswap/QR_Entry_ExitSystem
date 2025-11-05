import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import java.awt.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QRProject2 {

    private static final Set<String> entrySet = new HashSet<>();
    private static DefaultListModel<String> listModel;
    private static DefaultListModel<String> historyModel;
    private static final String LOG_DIR = "logs";

    public static void main(String[] args) {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.out.println("❌ No webcam detected!");
            return;
        }
        webcam.setViewSize(WebcamResolution.VGA.getSize());

        //MainFrame
        JFrame frame = new JFrame("💾 QR Code Entry/Exit System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        //BackgroundPanel
        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(20, 20));
        frame.setContentPane(mainPanel);

        //WebcamSetup
        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setMirrored(true);
        webcamPanel.setPreferredSize(new Dimension(500, 500));
        mainPanel.add(webcamPanel, BorderLayout.CENTER);

        //RightPanel Entries and Logs
        JPanel sidePanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setOpaque(false);
            }
        };
        sidePanel.setPreferredSize(new Dimension(400, 700));
        sidePanel.setOpaque(false);
        mainPanel.add(sidePanel, BorderLayout.EAST);

        //HeaderLabel
        JLabel title = new JLabel("Scan QR Code to Enter/Exit", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        mainPanel.add(title, BorderLayout.NORTH);

        //CustomComer
        Color darkPurple = new Color(60, 0, 90);

        //CurrentlyInside_List
        listModel = new DefaultListModel<>();
        JList<String> entryList = new JList<>(listModel);
        JScrollPane currentScroll = new JScrollPane(entryList);
        currentScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(darkPurple, 2),
                "Currently Inside",
                0, 0, new Font("Segoe UI", Font.BOLD, 18), darkPurple));

        //DailyLogs
        historyModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(darkPurple, 2),
                "Today's Log",
                0, 0, new Font("Segoe UI", Font.BOLD, 18), darkPurple));

        //Split Panel for Lists
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, currentScroll, historyScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        sidePanel.add(splitPane, BorderLayout.CENTER);

        // RefreshButton
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
                    BufferedImage image = webcam.getImage();
                    if (image == null) continue;

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    Result result = null;
                    try {
                        result = new MultiFormatReader().decode(bitmap);
                    } catch (NotFoundException ignored) {}

                    if (result != null) {
                        String qrData = result.getText().trim();
                        SwingUtilities.invokeLater(() -> updateLists(qrData));
                        Thread.sleep(2000);
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        scannerThread.setDaemon(true);
        scannerThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(webcam::close));
    }

    // Background
    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();
            Color color1 = new Color(16, 57, 165);
            Color color2 = new Color(54, 5, 74);
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
        }
    }

    // Buttons
    private static void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(250, 50));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(173, 216, 230));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE);
            }
        });
    }

    // UpdateList
    private static void updateLists(String qrData) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry;

        if (entrySet.contains(qrData)) {
            entrySet.remove(qrData);
            listModel.removeElement(qrData);
            logEntry = "EXIT  | " + qrData + " | " + timestamp;
            System.out.println("🚪 EXIT: " + qrData);
        } else {
            entrySet.add(qrData);
            listModel.addElement(qrData);
            logEntry = "ENTRY | " + qrData + " | " + timestamp;
            System.out.println("✅ ENTRY: " + qrData);
        }

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
                    historyModel.addElement(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void clearLogs() {
        entrySet.clear();
        listModel.clear();
        historyModel.clear();

        Path file = getTodayLogFile();
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, "✅ All entries and logs cleared for today!");
    }
}
