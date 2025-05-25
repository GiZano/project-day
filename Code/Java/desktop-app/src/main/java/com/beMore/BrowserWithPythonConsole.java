package com.beMore;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

public class BrowserWithPythonConsole {
    private static final Logger logger = LoggerFactory.getLogger(BrowserWithPythonConsole.class);
    private AppConfig config;
    private JFrame frame;
    private JSplitPane splitPane;
    private JTextArea outputArea;
    private JTextField inputField;
    private Process pythonProcess;
    private BufferedWriter processWriter;
    private BufferedReader processReader;
    private JPanel rightPanel;
    private JButton toggleConsoleButton;
    private boolean isConsoleVisible = true;
    private boolean isDarkTheme = false;
    private static int numeroInput;

    /**
     * Colors of the themes for the application: <br>
     * gray and white for light theme; <br>
     * blue navy for dark theme. <br>
     */
    private final ColorTheme lightTheme = new ColorTheme(
            Color.WHITE, Color.BLACK,
            new Color(240, 240, 240), new Color(220, 220, 220),
            new Color(180, 180, 180), new Color(150, 150, 150),
            new Color(245, 245, 245)
    );

    private final ColorTheme darkTheme = new ColorTheme(
            new Color(15, 23, 42), Color.WHITE,
            new Color(30, 41, 59), new Color(50, 61, 79),
            new Color(50, 60, 80), new Color(70, 80, 100),
            new Color(21, 30, 49)
    );

    private ColorTheme currentTheme = lightTheme;

    static {
        // Early initialization of JavaFX
        System.setProperty("javafx.embed.singleThread", "true");
        new JFXPanel();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new BrowserWithPythonConsole().initialize();
            } catch (Exception e) {
                logger.error("Errore durante l'inizializzazione", e);
                JOptionPane.showMessageDialog(null,
                        "Errore critico durante l'avvio: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /**
     * Initialize the application
     * @throws IOException error while accessing the configuration file.
     */
    private void initialize() throws IOException {
        loadConfiguration();
        createAndShowGUI();
        setupShutdownHook();
    }

    /**
     * Load configurations using the class AppConfig(). If it's impossible to load them from the class, read them from the configuration file.
     * @throws IOException error while accessing the configuration file.
     */
    private void loadConfiguration() throws IOException {
        config = new AppConfig();

        // Prima prova a caricare lo script dalle risorse
        try (InputStream input = getClass().getResourceAsStream("/Script/analisiv2.py")) {
            if (input != null) {
                // Crea un file temporaneo
                File tempFile = File.createTempFile("analisiv2", ".py");
                tempFile.deleteOnExit();
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                config.pythonScript = tempFile.getAbsolutePath();
                config.workingDir = System.getProperty("user.dir");
                logger.info("Script Python caricato dalle risorse in: {}", config.pythonScript);
                return;
            }
        }

        // If not found in resources folder, try to load from configuration file
        try {
            Properties props = new Properties();
            try (InputStream input = new FileInputStream("config.properties")) {
                props.load(input);
                config.pythonScript = props.getProperty("pythonScript", config.pythonScript);
                config.workingDir = props.getProperty("workingDir", config.workingDir);
                config.webUrl = props.getProperty("webUrl", config.webUrl);
            }
            logger.info("Configurazione caricata da file esterno");
        } catch (IOException e) {
            logger.warn("Configurazione non trovata, usando valori di default", e);
        }
    }

    /**
     * Function to shut down the process even if it's not terminated.
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                pythonProcess.destroyForcibly();
                logger.info("Processo Python terminato durante la chiusura");
            }
        }));
    }

    /**
     * Create the GUI with the three main components, apply the theme and show it on the screen.
     */
    private void createAndShowGUI() {
        initializeFrame();
        setupTopPanel();
        rightPanel = setupPythonConsole();
        initJavaFXComponents();
        applyTheme(currentTheme);
        frame.setVisible(true);
    }

    /**
     * Initialization of the frame.
     */
    private void initializeFrame() {
        frame = new JFrame("bE More - I.S. Archimede");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setMinimumSize(new Dimension(800, 600));
        setWindowIcon();
    }

    /**
     * Set the icon with the one in the resources' folder.
     */
    private void setWindowIcon() {
        try (InputStream iconStream = getClass().getResourceAsStream("/logo.png")) {
            if (iconStream != null) {
                frame.setIconImage(ImageIO.read(iconStream));
            } else {
                logger.warn("Icona non trovata nelle risorse");
            }
        } catch (Exception e) {
            logger.error("Errore nel caricamento dell'icona", e);
        }
    }

    /**
     * Creation of the top panel with the buttons to open the web browser, change theme and hide/show the console panel.
     */
    private void setupTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(3, 3, 3, 3));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        leftPanel.add(createButton("Apri browser", _ -> openInSystemBrowser()));

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        centerPanel.add(createButton("Tema", _ -> toggleTheme()));

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        toggleConsoleButton = createButton("Nascondi", _ -> toggleConsoleVisibility());
        rightButtonPanel.add(toggleConsoleButton);

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(centerPanel, BorderLayout.CENTER);
        topPanel.add(rightButtonPanel, BorderLayout.EAST);

        frame.add(topPanel, BorderLayout.NORTH);
    }

    /**
     * Function to create a JButton to put inside the frame.
     * @param text Text shown on the button
     * @param action action to listen to for activating the relative function
     * @return a JButton
     */
    private JButton createButton(String text, ActionListener action) {
        JButton button = new JButton(text) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = 24;
                d.width += 8;
                return d;
            }
        };

        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.addActionListener(action);

        int radius = 8;
        Border normalBorder = new CompoundBorder(
                new RoundBorder(radius, currentTheme.buttonBorder(), 1),
                new EmptyBorder(2, 8, 2, 8)
        );

        Border hoverBorder = new CompoundBorder(
                new RoundBorder(radius, currentTheme.buttonHoverBorder(), 1),
                new EmptyBorder(2, 8, 2, 8)
        );

        button.setBorder(normalBorder);
        button.setBackground(currentTheme.buttonBg());
        button.setForeground(currentTheme.fg());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(currentTheme.buttonHoverBg());
                button.setBorder(hoverBorder);
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(currentTheme.buttonBg());
                button.setBorder(normalBorder);
                button.setCursor(Cursor.getDefaultCursor());
            }
        });

        return button;
    }

    private void initJavaFXComponents() {
        JFXPanel fxPanel = new JFXPanel();
        frame.add(fxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            try {
                WebView webView = new WebView();
                webView.getEngine().load(config.getWebUrl());
                fxPanel.setScene(new Scene(webView));

                SwingUtilities.invokeLater(() -> {
                    frame.remove(fxPanel);
                    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fxPanel, rightPanel);
                    splitPane.setResizeWeight(0.7);
                    frame.add(splitPane, BorderLayout.CENTER);
                    frame.revalidate();
                });
            } catch (Exception e) {
                logger.error("Errore nell'inizializzazione di JavaFX", e);
                JOptionPane.showMessageDialog(frame,
                        "Errore nell'inizializzazione del browser: " + e.getMessage(),
                        "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Set up the console panel connected to the python process present in the resources folder, used to analyze collected data.
     * @return JPanel containing the console to control the Python process.
     */
    private JPanel setupPythonConsole() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        topPanel.add(createButton("Riavvia", _ -> restartPythonProcess()));
        panel.add(topPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(2, 0));
        inputField = new JTextField();
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 11));
        inputField.addActionListener(this::sendInputToPython);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(createButton("Invia", this::sendInputToPython), BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        startPythonProcess();
        return panel;
    }

    /**
     * Restart the python process to do a new analysis.
     */
    private void restartPythonProcess() {
        outputArea.setText("");
        startPythonProcess();
    }

    /**
     * Function to check if the file containing the python analyzer script exists
     * @return true/false if the file exists or not.
     */
    private boolean checkPythonScriptExists() {
        File scriptFile = new File(config.getPythonScript());
        if (!scriptFile.exists()) {
            logger.error("File Python non trovato: {}", config.getPythonScript());
            SwingUtilities.invokeLater(() -> {
                outputArea.append("ERRORE: File Python non trovato in " + config.getPythonScript() + "\n");
                outputArea.append("Cercato in: " + scriptFile.getAbsolutePath() + "\n");
            });
            return false;
        }
        return true;
    }

    /**
     * Start the python process to receive input from the console and then analyze collected data.
     */
    private void startPythonProcess() {
        if (!checkPythonScriptExists()) {
            return;
        }

        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroyForcibly();
        }

        try {
            numeroInput = 0;
            // Get the absolute path of the working directory
            File workingDir = new File("X\\Ollama");

            // Check if directory exists
            if (!workingDir.exists()) {
                SwingUtilities.invokeLater(() -> outputArea.append("ERRORE: Directory di lavoro non trovata: " + workingDir + "\n"));
                return;
            }

            // Create the ProcessBuilder with the full path
            ProcessBuilder pb = new ProcessBuilder("python", config.getPythonScript());
            pb.directory(workingDir);
            pb.redirectErrorStream(true);

            // Add environment variables if needed
            Map<String, String> env = pb.environment();
            env.put("PYTHONPATH", workingDir.getAbsolutePath());

            logger.info("Avvio processo Python con comando: {}", String.join(" ", pb.command()));
            logger.info("Directory di lavoro: {}", pb.directory());
            logger.info("Percorso script: {}", config.getPythonScript());

            pythonProcess = pb.start();

            processWriter = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream()));
            processReader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));

            new Thread(() -> {
                outputArea.append("Processo Python avviato\n");

                try {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        String finalLine = processOutputLine(line);
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append(finalLine + "\n");
                            outputArea.setCaretPosition(outputArea.getDocument().getLength());
                        });
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() ->
                            outputArea.append("Errore lettura output: " + e.getMessage() + "\n"));
                    logger.error("Errore nella lettura dell'output Python", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    int exitCode = pythonProcess.waitFor();
                    SwingUtilities.invokeLater(() ->
                            outputArea.append("\nProcesso terminato con codice: " + exitCode + "\n"));
                    logger.info("Processo Python terminato con codice {}", exitCode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Thread di monitoraggio interrotto", e);
                }
            }).start();

        } catch (IOException e) {
            logger.error("Errore nell'avvio del processo Python", e);
            SwingUtilities.invokeLater(() -> outputArea.append("Errore avvio processo: " + e.getMessage() + "\n"));
        }
    }

    /**
     * Skip the line with unreadable characters, which should be the ones showing the analysis is loading.
     * @param line the line received from the python process.
     * @return the same line unless it is the 9th line.
     */
    private String processOutputLine(String line) {
        if(++numeroInput == 9){
            return "";
        }
        return line;
    }

    /**
     * Take the input from the JTextField and send it to the python process in background.
     * @param e event causing the function to be called.
     */
    private void sendInputToPython(ActionEvent e) {
        String input = inputField.getText();
        if (input.isEmpty()) return;

        try {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                outputArea.append("> " + input + "\n");
                processWriter.write(input + "\n");
                processWriter.flush();
                inputField.setText("");
            } else {
                outputArea.append("Processo Python non attivo\n");
            }
        } catch (IOException ex) {
            outputArea.append("Errore invio input: " + ex.getMessage() + "\n");
            logger.error("Errore nell'invio dell'input a Python", ex);
        }
    }

    /**
     * Switch between light and dark theme.
     */
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        currentTheme = isDarkTheme ? darkTheme : lightTheme;
        applyTheme(currentTheme);
    }

    /**
     * Apply the theme to the frame and call the function to apply it to the components.
     * @param theme the theme to apply.
     */
    private void applyTheme(ColorTheme theme) {
        frame.getContentPane().setBackground(theme.bg());
        applyThemeToComponents(frame.getContentPane(), theme);
    }

    /**
     * Apply the theme to the components of the frame application.
     * @param component component to apply the theme.
     * @param theme theme to apply.
     */
    private void applyThemeToComponents(Component component, ColorTheme theme) {
        if (component instanceof JPanel panel) {
            panel.setBackground(panel.isOpaque() ? theme.panelBg() : theme.bg());
            for (Component child : panel.getComponents()) {
                applyThemeToComponents(child, theme);
            }
        } else if (component instanceof JButton button) {
            button.setBackground(theme.buttonBg());
            button.setForeground(theme.fg());
        } else if (component instanceof JTextArea textArea) {
            textArea.setBackground(theme.bg());
            textArea.setForeground(theme.fg());
            textArea.setCaretColor(theme.fg());
        } else if (component instanceof JTextField textField) {
            textField.setBackground(theme.bg());
            textField.setForeground(theme.fg());
            textField.setCaretColor(theme.fg());
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.buttonBorder(), 1),
                    BorderFactory.createEmptyBorder(3, 3, 3, 3)
            ));
        } else if (component instanceof JScrollPane scrollPane) {
            scrollPane.getViewport().setBackground(theme.bg());
            scrollPane.setBorder(BorderFactory.createLineBorder(theme.buttonBorder(), 1));
        } else if (component instanceof JSplitPane splitPane) {
            splitPane.setBackground(theme.bg());
        }
    }

    /**
     * Hide the console if it's being shown. <br>
     * Show the console if it's being hidden.
     */
    private void toggleConsoleVisibility() {
        isConsoleVisible = !isConsoleVisible;
        if (isConsoleVisible) {
            splitPane.setRightComponent(rightPanel);
            toggleConsoleButton.setText("Nascondi");
            splitPane.setDividerLocation(0.7);
        } else {
            splitPane.setRightComponent(null);
            toggleConsoleButton.setText("Mostra");
        }
        frame.revalidate();
    }

    /**
     * Open the client view of ThingsBoard directly on the web browser.
     */
    private void openInSystemBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(config.getWebUrl()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "Impossibile aprire il browser: " + e.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            logger.error("Errore nell'apertura del browser di sistema", e);
        }
    }

    private record ColorTheme(
            Color bg, Color fg,
            Color buttonBg, Color buttonHoverBg,
            Color buttonBorder, Color buttonHoverBorder,
            Color panelBg
    ) {}

    /**
     * Class containing the configuration data of the application.
     */
    private static class AppConfig {
        private String pythonScript;
        private String workingDir;
        private String webUrl;

        public AppConfig() {
            this.pythonScript = "analisiv2.py";
            this.workingDir = "X\\Ollama";
            this.webUrl = "your_thingsboard_server_ip:8080/";
        }

        public String getPythonScript() { return pythonScript; }
        public String getWorkingDir() { return workingDir; }
        public String getWebUrl() { return webUrl; }
    }

    private static class RoundBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        private final int thickness;

        public RoundBorder(int radius, Color color, int thickness) {
            this.radius = radius;
            this.color = color;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius, radius, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = radius;
            return insets;
        }
    }
}