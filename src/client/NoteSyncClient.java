package client;

import common.models.*;
import common.network.*;
import common.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoteSyncClient extends JFrame {
    private static final Logger logger = LoggerUtil.getLogger(NoteSyncClient.class);

    private final ConfigManager config = ConfigManager.getInstance();
    private final String clientId = Utils.generateId();
    private final String clientName;

    private TCPConnection serverConnection;
    private UDPConnection udpConnection;
    private final Map<String, Note> localNotes = new ConcurrentHashMap<>();
    private boolean isConnected = false;

    // GUI
    private final DefaultListModel<Note> notesListModel = new DefaultListModel<>();
    private final JList<Note> notesList = new JList<>(notesListModel);
    private final JTextField titleField = new JTextField();
    private final JTextArea contentArea = new JTextArea();
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final JButton createButton = new JButton("Create");
    private final JButton updateButton = new JButton("Update");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton syncButton = new JButton("Sync");
    private final JLabel statusLabel = new JLabel("Disconnected");
    private javax.swing.Timer heartbeatTimer;

    public NoteSyncClient(String clientName) {
        this.clientName = (clientName != null && !clientName.trim().isEmpty())
                ? clientName.trim() : ("Client-" + clientId.substring(0, 8));
        initializeGUI();
        setupEventHandlers();
        logger.info("NoteSyncClient initialized: " + this.clientName);
    }

    private void initializeGUI() {
        setTitle("Note Sync Client - " + clientName);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Top
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(connectButton); top.add(disconnectButton); top.add(syncButton);
        top.add(Box.createHorizontalStrut(16)); top.add(new JLabel("Status:")); top.add(statusLabel);
        add(top, BorderLayout.NORTH);

        // Left (list)
        notesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notesList.setCellRenderer(new NoteCellRenderer());
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Notes"));
        left.setPreferredSize(new Dimension(300, 0));
        left.add(new JScrollPane(notesList), BorderLayout.CENTER);

        // Right (editor)
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Note Editor"));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.add(new JLabel("Title:"), BorderLayout.WEST);
        titleRow.add(titleField, BorderLayout.CENTER);
        right.add(titleRow, BorderLayout.NORTH);
        contentArea.setLineWrap(true); contentArea.setWrapStyleWord(true);
        right.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout());
        actions.add(createButton); actions.add(updateButton); actions.add(deleteButton);
        right.add(actions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(300);
        add(split, BorderLayout.CENTER);

        // Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Client ID: " + clientId.substring(0, 8) + "..."));
        add(bottom, BorderLayout.SOUTH);

        updateButton.setEnabled(false); deleteButton.setEnabled(false);
        disconnectButton.setEnabled(false); syncButton.setEnabled(false);
        setLocationRelativeTo(null);
    }

    private void setupEventHandlers() {
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { disconnect(); System.exit(0); } });
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        syncButton.addActionListener(e -> requestSync());
        createButton.addActionListener(e -> createNote());
        updateButton.addActionListener(e -> updateNote());
        deleteButton.addActionListener(e -> deleteNote());
        notesList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectNote(notesList.getSelectedValue()); });
    }

    private void connect() {
        connectButton.setEnabled(false);
        statusLabel.setText("Connecting...");

        new SwingWorker<TCPConnection, String>() {
            @Override protected TCPConnection doInBackground() throws Exception {
                publish("Connecting to server...");
                TCPConnection tcp = new TCPConnection(config.getServerHost(), config.getTcpPort());
                publish("TCP connected");
                udpConnection = new UDPConnection();
                udpConnection.setMessageHandler(new UDPHandler());
                udpConnection.startListening();
                publish("UDP listening");
                ClientInfo info = new ClientInfo(clientId, clientName,
                        udpConnection.getLocalAddress().getHostAddress(), udpConnection.getLocalPort());
                tcp.sendMessage(new Message(MessageType.CLIENT_CONNECT, clientId, info));
                publish("Connect sent");
                return tcp;
            }
            @Override protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) statusLabel.setText(chunks.get(chunks.size() - 1));
            }
            @Override protected void done() {
                try {
                    serverConnection = get();
                    serverConnection.setMessageHandler(new ServerHandler());
                    serverConnection.startCommunication();
                    isConnected = true; updateGUIState();
                    statusLabel.setText("Connected to " + config.getServerHost() + ":" + config.getTcpPort());
                    startHeartbeat();
                    logger.info("Connected to server successfully");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    logger.log(Level.SEVERE, "Connection failed", cause);
                    cleanupConnections();
                    isConnected = false; updateGUIState();
                    statusLabel.setText("Connection failed");
                    JOptionPane.showMessageDialog(NoteSyncClient.this,
                            "Cannot connect to " + config.getServerHost() + ":" + config.getTcpPort() +
                                    "\nCheck server status, address/port, firewall.\n\nError: " + cause.getMessage(),
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void disconnect() {
        stopHeartbeat();
        if (isConnected && serverConnection != null)
            serverConnection.sendMessage(new Message(MessageType.CLIENT_DISCONNECT, clientId, null));
        cleanupConnections();
        isConnected = false; updateGUIState();
        statusLabel.setText("Disconnected");
        logger.info("Disconnected from server");
    }

    private void cleanupConnections() {
        try { if (serverConnection != null) serverConnection.close(); } catch (Exception ignore) {}
        try { if (udpConnection != null) udpConnection.stop(); } catch (Exception ignore) {}
        serverConnection = null; udpConnection = null;
    }

    private void sendIfConnected(MessageType type, Object payload) {
        if (isConnected && serverConnection != null) serverConnection.sendMessage(new Message(type, clientId, payload));
    }

    private void createNote() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter a title", "Error", JOptionPane.ERROR_MESSAGE); return; }
        Note note = new Note(title, contentArea.getText().trim(), clientId);
        localNotes.put(note.getId(), note); notesListModel.addElement(note);
        sendIfConnected(MessageType.NOTE_CREATE, note);
        titleField.setText(""); contentArea.setText("");
        logger.info("Note created: " + note.getId());
    }

    private void updateNote() {
        Note n = notesList.getSelectedValue(); if (n == null) return;
        String title = titleField.getText().trim();
        if (title.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter a title", "Error", JOptionPane.ERROR_MESSAGE); return; }
        n.setTitle(title); n.setContent(contentArea.getText().trim()); n.updateLastModified();
        notesList.repaint(); sendIfConnected(MessageType.NOTE_UPDATE, n);
        logger.info("Note updated: " + n.getId());
    }

    private void deleteNote() {
        Note n = notesList.getSelectedValue(); if (n == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete this note?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            localNotes.remove(n.getId()); notesListModel.removeElement(n);
            sendIfConnected(MessageType.NOTE_DELETE, n.getId());
            selectNote(null); logger.info("Note deleted");
        }
    }

    private void selectNote(Note note) {
        titleField.setText(note != null ? note.getTitle() : "");
        contentArea.setText(note != null ? note.getContent() : "");
        boolean has = note != null; updateButton.setEnabled(has); deleteButton.setEnabled(has);
    }

    private void requestSync() {
        SyncRequest req = new SyncRequest(clientId, 0); req.setFullSync(true);
        sendIfConnected(MessageType.SYNC_REQUEST, req);
        logger.info("Sync requested");
    }

    private void startHeartbeat() {
        stopHeartbeat();
        int interval = Math.max(1000, config.getHeartbeatInterval());
        heartbeatTimer = new javax.swing.Timer(interval, e -> {
            if (isConnected && serverConnection != null && serverConnection.isConnected())
                sendIfConnected(MessageType.HEARTBEAT, "ping");
            else stopHeartbeat();
        });
        heartbeatTimer.setInitialDelay(0); heartbeatTimer.start();
        logger.info("Heartbeat started.");
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) { heartbeatTimer.stop(); heartbeatTimer = null; logger.info("Heartbeat stopped."); }
    }

    private void updateGUIState() {
        connectButton.setEnabled(!isConnected);
        disconnectButton.setEnabled(isConnected);
        syncButton.setEnabled(isConnected);
    }

    // Handlers
    private class ServerHandler implements TCPConnection.MessageHandler {
        private final Map<MessageType, Consumer<Message>> handlers = new EnumMap<>(MessageType.class);
        ServerHandler() {
            handlers.put(MessageType.CONNECT_ACK, m -> logger.info("Server ACK connection"));
            handlers.put(MessageType.SYNC_RESPONSE, this::onSyncResponse);
            handlers.put(MessageType.NOTE_CREATED, m -> onNoteUpsert(m.getPayload(Note.class), true));
            handlers.put(MessageType.NOTE_UPDATED, m -> onNoteUpsert(m.getPayload(Note.class), false));
            handlers.put(MessageType.NOTE_DELETED, m -> onNoteDeleted(m.getPayload(String.class)));
            handlers.put(MessageType.ERROR, this::onServerError);
        }
        @Override public void handleMessage(Message m) { handlers.getOrDefault(m.getType(), x -> logger.warning("Unknown: " + x.getType())).accept(m); }
        private void onSyncResponse(Message m) {
            SyncResponse r = m.getPayload(SyncResponse.class);
            if (r != null && r.isSuccess()) {
                SwingUtilities.invokeLater(() -> {
                    notesListModel.clear(); localNotes.clear();
                    for (Note n : r.getNotes()) { localNotes.put(n.getId(), n); notesListModel.addElement(n); }
                    selectNote(null); logger.info("Synced " + r.getNotes().size() + " notes");
                });
            }
        }
        private void onNoteUpsert(Note n, boolean isNew) {
            if (n == null || clientId.equals(n.getAuthorId())) return;
            SwingUtilities.invokeLater(() -> {
                localNotes.put(n.getId(), n);
                if (isNew && !containsNote(n.getId())) notesListModel.addElement(n);
                else replaceNoteInList(n);
                if (notesList.getSelectedValue() != null && notesList.getSelectedValue().getId().equals(n.getId())) selectNote(n);
            });
        }
        private boolean containsNote(String id) { for (int i = 0; i < notesListModel.size(); i++) if (notesListModel.get(i).getId().equals(id)) return true; return false; }
        private void replaceNoteInList(Note n) { for (int i = 0; i < notesListModel.size(); i++) if (notesListModel.get(i).getId().equals(n.getId())) { notesListModel.set(i, n); break; } }
        private void onNoteDeleted(String id) {
            if (id == null) return; SwingUtilities.invokeLater(() -> {
                Note removed = localNotes.remove(id); if (removed != null) notesListModel.removeElement(removed);
                if (notesList.getSelectedValue() != null && id.equals(notesList.getSelectedValue().getId())) selectNote(null);
            });
        }
        private void onServerError(Message m) {
            String err = m.getPayload(String.class);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(NoteSyncClient.this, "Server error: " + err, "Server Error", JOptionPane.ERROR_MESSAGE));
        }
        @Override public void onConnectionClosed() { onConnLost("Connection to server lost", "Connection Lost"); }
        @Override public void onConnectionError(Exception e) { onConnLost("Connection error: " + e.getMessage(), "Connection Error"); }
        private void onConnLost(String msg, String title) {
            SwingUtilities.invokeLater(() -> { stopHeartbeat(); isConnected = false; updateGUIState(); statusLabel.setText(title); JOptionPane.showMessageDialog(NoteSyncClient.this, msg, title, JOptionPane.WARNING_MESSAGE); });
        }
    }

    private static class UDPHandler implements UDPConnection.MessageHandler {
        @Override public void handleMessage(Message message, java.net.InetAddress sender, int senderPort) {}
        @Override public void onError(Exception e) { LoggerUtil.getLogger(UDPHandler.class).log(Level.WARNING, "UDP error", e); }
    }

    private static class NoteCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Note) { Note n = (Note) value; setText(n.getTitle()); setToolTipText(n.getTitle() + " - " + Utils.formatDateTime(n.getLastModified())); }
            return this;
        }
    }

    public static void main(String[] args) {
        LoggerUtil.initializeLogging();
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { logger.warning("Could not set system look and feel"); }
        String name = (args.length > 0) ? args[0] : JOptionPane.showInputDialog(null, "Enter client name:", "Note Sync Client", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> new NoteSyncClient(name.trim()).setVisible(true));
    }
}