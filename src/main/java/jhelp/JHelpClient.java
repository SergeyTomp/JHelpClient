package jhelp;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.JOptionPane.*;
import static jhelp.Operation.*;

public class JHelpClient extends JFrame {

    private JPanel contentPane;
    private JTextField termTextField;
    private JButton findButton;
    private JButton editButton;
    private JButton nextButton;
    private JButton previousButton;
    private JButton addButton;
    private JButton deleteButton;
    private JButton exitButton;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu settingsMenu;
    private JMenu helpMenu;
    private JTextPane definitionTextPane;
    private JButton clearFormButton;

    private int SERVER_PORT = 16105;
    private final String NOT_FOUND = "Definition not found";
    private List<String> foundDefinitions = new ArrayList<>();
    private static Logger log = LoggerFactory.getLogger(JHelpClient.class);

    public void setSERVER_PORT(int SERVER_PORT) {
        this.SERVER_PORT = SERVER_PORT;
    }

    public JHelpClient(){
        super("JHelpClient");
        $$$setupUI$$$();
        setContentPane(contentPane);
        pack();
        initWindow();
//        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        exitButton.addActionListener(e -> System.exit(0));

        findButton.addActionListener(e -> {
            editButton.setEnabled(false);
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            deleteButton.setEnabled(false);
            definitionTextPane.setEditable(false);
            String request = termTextField.getText().trim();
            if(request.isEmpty()) return;
            foundDefinitions.clear();

            try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {
                log.info("New find socket created at {}", InetAddress.getLocalHost() + ":" + SERVER_PORT );
                try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    ous.writeObject(FIND);
                    ous.writeObject(request);
                    ous.flush();

                    String def = (String) ois.readObject();
                    if (def == null)
                        definitionTextPane.setText(NOT_FOUND);
                    else {
                        foundDefinitions.clear();
                        editButton.setEnabled(true);
                        foundDefinitions.add(def);
                        while (true) {
                            def = (String) ois.readObject();
                            foundDefinitions.add(def);
                        }
                    }
                }
            } catch (EOFException eof) {
                showFirstDefinition();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });

        editButton.addActionListener(e -> {
            if (!termTextField.getText().isEmpty() && !definitionTextPane.getText().isEmpty()) {
                definitionTextPane.setEditable(true);
            }
            try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {
                log.info("New edit socket created at {}", InetAddress.getLocalHost() + ":" + SERVER_PORT );
                try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    ous.writeObject(EDIT);
                    ous.writeObject(termTextField.getText());
                    ous.writeObject(definitionTextPane.getText());
                    ous.flush();

                    Operation response = (Operation) ois.readObject();
                    switch(response){
                        case SUCCESS:
                            showInfo("Current definition was deleted from DB, add a new one if needed.", "Success");
//                            editButton.setEnabled(false);
                            break;
                        case FAILED:
                            showInfo("Operation failed: specified term or definition were not found.", "Fail");
//                            editButton.setEnabled(false);
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });

        clearFormButton.addActionListener(e -> {
            definitionTextPane.setText("");
            termTextField.setText("");
            editButton.setEnabled(false);
            nextButton.setEnabled(false);
            previousButton.setEnabled(false);
            deleteButton.setEnabled(false);
            addButton.setEnabled(false);
            findButton.setEnabled(false);
            clearFormButton.setEnabled(false);
        });

        termTextField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                addButton.setEnabled(true);
                findButton.setEnabled(true);
                clearFormButton.setEnabled(true);
                deleteButton.setEnabled(true);
                definitionTextPane.setEditable(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (termTextField.getText().isEmpty()) {
                    addButton.setEnabled(false);
                    findButton.setEnabled(false);
                    clearFormButton.setEnabled(false);
                }
            }
        });

        nextButton.addActionListener(e -> {
            editButton.setEnabled(true);
            int i = foundDefinitions.indexOf(definitionTextPane.getText());
            if (i != foundDefinitions.size() - 1) {
                definitionTextPane.setText(foundDefinitions.get(++i));
            }
            if (i == 1) {
                previousButton.setEnabled(true);
            }
            if (i == foundDefinitions.size() - 1) {
                nextButton.setEnabled(false);
            }
        });

        previousButton.addActionListener(e -> {
            editButton.setEnabled(true);
            int i = foundDefinitions.indexOf(definitionTextPane.getText());
            if (i != 0) {
                definitionTextPane.setText(foundDefinitions.get(--i));
            }
            if (i == foundDefinitions.size() - 2) {
                nextButton.setEnabled(true);
            }
            if (i == 0) {
                previousButton.setEnabled(false);
            }
        });

        addButton.addActionListener(e -> {
            definitionTextPane.setEditable(false);
            String term = termTextField.getText().trim();
            String definition = definitionTextPane.getText().trim();

            if (term.isEmpty() && definition.isEmpty()) {
                showWarning("Nothing to add!", "Empty arguments");
                return;
            } else if (term.isEmpty()) {
                showWarning("Term can not be empty!", "Empty term");
                return;
            } else if (definition.isEmpty()) {
                showWarning("Definition can not be empty!", "Empty definition");
                return;
            }
            try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {
                log.info("New add socket created at {}", InetAddress.getLocalHost() + ":" + SERVER_PORT );
                try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    ous.writeObject(ADD);
                    ous.writeObject(term);
                    ous.writeObject(definition);
                    ous.flush();

                    Operation response = (Operation) ois.readObject();
                    switch(response){
                        case SUCCESS:
                            showInfo("New data added to DB.", "Success");
                            break;
                        case FAILED:
                            showInfo("Operation failed: specified definition already exists in DB.", "Fail");
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            findButton.doClick();
        });

        deleteButton.addActionListener(e -> {
            definitionTextPane.setEditable(false);
            String term = termTextField.getText().trim();
            if (term.isEmpty()) {
                showWarning("Term can not be empty", "Empty term");
                return;}
            try (Socket socket = new Socket(InetAddress.getLocalHost(), SERVER_PORT)) {
                log.info("New delete socket created at {}", InetAddress.getLocalHost() + ":" + SERVER_PORT );
                try (ObjectOutputStream ous = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                    ous.writeObject(DELETE);
                    ous.writeObject(term);
                    ous.flush();

                    Operation response = (Operation) ois.readObject();
                    switch(response){
                        case SUCCESS:
                            showInfo("Term was deleted from DB.", "Success");
                            break;
                        case FAILED:
                            showInfo("Operation failed: specified term was not found in DB", "Fail");
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void initWindow() {
        editButton.setEnabled(false);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);
        deleteButton.setEnabled(false);
        addButton.setEnabled(false);
        findButton.setEnabled(false);
        clearFormButton.setEnabled(false);
        definitionTextPane.setEditable(false);
    }

    private void showFirstDefinition(){
        editButton.setEnabled(true);
        clearFormButton.setEnabled(true);
        if(foundDefinitions.size() > 1){
            nextButton.setEnabled(true);
        }
        definitionTextPane.setText(foundDefinitions.get(0));
    }

    private void showWarning(String message, String title) {
        showMessageDialog(this, message, title, WARNING_MESSAGE);
    }

    private void showInfo(String message, String title) {
        showMessageDialog(this, message, title, INFORMATION_MESSAGE);
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(JHelpClient::new);
//    }

    private void createUIComponents() {
        // TODO: place custom component creation code here

    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setMinimumSize(new Dimension(-1, -1));
        panel1.setName("");
        panel1.setPreferredSize(new Dimension(-1, -1));
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(contentPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        contentPane.setBorder(BorderFactory.createTitledBorder(""));
        menuBar = new JMenuBar();
        menuBar.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(menuBar, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(-1, 30), new Dimension(-1, 30), new Dimension(-1, 30), 0, false));
        editMenu = new JMenu();
        editMenu.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        editMenu.setLabel("editMenu");
        editMenu.setName("editMenu");
        editMenu.setText("editMenu");
        menuBar.add(editMenu, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fileMenu = new JMenu();
        fileMenu.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        fileMenu.setLabel("fileMenu");
        fileMenu.setName("fileMenu");
        menuBar.add(fileMenu, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        menuBar.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, new Dimension(-1, 30), 0, false));
        settingsMenu = new JMenu();
        settingsMenu.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsMenu.setLabel("settingsMenu");
        settingsMenu.setName("settingsMenu");
        menuBar.add(settingsMenu, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        helpMenu = new JMenu();
        helpMenu.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        helpMenu.setLabel("helpMenu");
        helpMenu.setName("helpMenu");
        menuBar.add(helpMenu, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        contentPane.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(11, 2, new Insets(0, 0, 0, 20), -1, -1));
        panel2.setMinimumSize(new Dimension(-1, -1));
        panel2.setPreferredSize(new Dimension(800, 500));
        tabbedPane1.addTab("Main", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.setBackground(new Color(-8091351));
        panel3.setForeground(new Color(-4490888));
        panel2.add(panel3, new GridConstraints(1, 0, 10, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        termTextField = new JTextField();
        panel2.add(termTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 30), new Dimension(-1, 30), new Dimension(-1, 30), 0, false));
        findButton = new JButton();
        findButton.setText("Find");
        panel2.add(findButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editButton = new JButton();
        editButton.setText("editMenu");
        panel2.add(editButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(105, 30), null, 0, false));
        nextButton = new JButton();
        nextButton.setText("Next");
        panel2.add(nextButton, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(105, 30), null, 0, false));
        previousButton = new JButton();
        previousButton.setText("Previous");
        panel2.add(previousButton, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setText("Add");
        panel2.add(addButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(105, 30), null, 0, false));
        deleteButton = new JButton();
        deleteButton.setText("Delete");
        panel2.add(deleteButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel2.add(spacer4, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        exitButton = new JButton();
        exitButton.setText("Exit");
        panel2.add(exitButton, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel2.add(spacer5, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setMinimumSize(new Dimension(-1, -1));
        panel4.setPreferredSize(new Dimension(-1, -1));
        tabbedPane1.addTab("settingsMenu", panel4);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setMinimumSize(new Dimension(-1, -1));
        panel5.setPreferredSize(new Dimension(-1, -1));
        panel5.setRequestFocusEnabled(true);
        tabbedPane1.addTab("helpMenu", panel5);
    }
}
