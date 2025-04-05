import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * View class that manages UI components and interactions
 */
class CalendarView {
    private JFrame frame;
    private JTable eventTable;
    private DefaultTableModel tableModel;
    private CalendarController controller;
    private JPanel calendarPanel;
    private JComboBox<String> categoryFilterBox;
    private JTextField searchField;
    private JDatePickerImpl startDatePicker;
    private JDatePickerImpl endDatePicker;
    private JCheckBox showPastEventsCheckbox;
    private JLabel monthYearLabel;
    private JPanel monthViewPanel;
    private Color[] categoryColors = {
            new Color(255, 200, 200), // Light red
            new Color(200, 255, 200), // Light green
            new Color(200, 200, 255), // Light blue
            new Color(255, 255, 200), // Light yellow
            new Color(255, 200, 255)  // Light purple
    };

    public CalendarView(CalendarController controller) {
        this.controller = controller;
        createAndShowGUI();
    }

    private void createAndShowGUI() {
        frame = new JFrame("Enhanced Calendar App");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
                frame.dispose();
                System.exit(0);
            }
        });
        frame.setSize(1000, 700);
        frame.setMinimumSize(new Dimension(800, 600));

        // Create the main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(350);

        // Calendar view on the left
        JPanel leftPanel = createCalendarPanel();

        // Event list on the right
        JPanel rightPanel = createEventListPanel();

        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);

        frame.add(mainSplitPane, BorderLayout.CENTER);
        frame.add(createToolbar(), BorderLayout.NORTH);
        frame.add(createStatusBar(), BorderLayout.SOUTH);

        frame.setVisible(true);

        // Set divider locations after components are visible
        SwingUtilities.invokeLater(() -> {
            mainSplitPane.setDividerLocation(0.35);
        });
    }

    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        // Month navigation panel
        JPanel navPanel = new JPanel(new BorderLayout());
        JButton prevButton = new JButton("◀");
        JButton nextButton = new JButton("▶");
        JButton todayButton = new JButton("Today");
        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel navButtonsPanel = new JPanel();
        navButtonsPanel.add(prevButton);
        navButtonsPanel.add(todayButton);
        navButtonsPanel.add(nextButton);

        navPanel.add(monthYearLabel, BorderLayout.CENTER);
        navPanel.add(navButtonsPanel, BorderLayout.SOUTH);

        // Calendar grid
        monthViewPanel = new JPanel(new GridLayout(0, 7));
        calendarPanel = new JPanel(new BorderLayout());
        calendarPanel.add(createCalendarHeader(), BorderLayout.NORTH);
        calendarPanel.add(monthViewPanel, BorderLayout.CENTER);

        panel.add(navPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(calendarPanel), BorderLayout.CENTER);

        // Event handlers for navigation buttons
        prevButton.addActionListener(e -> {
            LocalDate currentMonth = controller.getCurrentDisplayMonth();
            controller.setCurrentDisplayMonth(currentMonth.minusMonths(1));
            updateCalendarPanel(controller.getEvents());
        });

        nextButton.addActionListener(e -> {
            LocalDate currentMonth = controller.getCurrentDisplayMonth();
            controller.setCurrentDisplayMonth(currentMonth.plusMonths(1));
            updateCalendarPanel(controller.getEvents());
        });

        todayButton.addActionListener(e -> {
            controller.setCurrentDisplayMonth(LocalDate.now());
            updateCalendarPanel(controller.getEvents());
        });

        return panel;
    }

    private JPanel createCalendarHeader() {
        JPanel header = new JPanel(new GridLayout(1, 7));
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (String day : dayNames) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
            header.add(label);
        }

        return header;
    }

    public void updateCalendarPanel(java.util.List<CalendarEvent> events) {
        monthViewPanel.removeAll();

        LocalDate currentMonth = controller.getCurrentDisplayMonth();
        YearMonth yearMonth = YearMonth.from(currentMonth);
        LocalDate firstDay = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();

        // Update the month/year label
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthYearLabel.setText(firstDay.format(formatter));

        // Get the day of week for the first day (0 = Sunday, 6 = Saturday)
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;

        // Add empty cells for days before the first day of the month
        for (int i = 0; i < firstDayOfWeek; i++) {
            monthViewPanel.add(createEmptyDayPanel());
        }

        // Add cells for each day of the month
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            monthViewPanel.add(createDayPanel(date, events));
        }

        // Add empty cells to fill the last row if needed
        int totalCells = monthViewPanel.getComponentCount();
        int remainingCells = 42 - totalCells; // 6 rows of 7 days
        if (remainingCells > 7) remainingCells = remainingCells - 7; // Keep it at 6 rows max

        for (int i = 0; i < remainingCells; i++) {
            monthViewPanel.add(createEmptyDayPanel());
        }

        monthViewPanel.revalidate();
        monthViewPanel.repaint();
    }

    private JPanel createEmptyDayPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        return panel;
    }

    private JPanel createDayPanel(LocalDate date, java.util.List<CalendarEvent> events) {
        JPanel dayPanel = new JPanel(new BorderLayout());
        dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // Check if this is today
        boolean isToday = date.equals(LocalDate.now());

        // Day number label
        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.RIGHT);
        dayLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        if (isToday) {
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            dayLabel.setForeground(new Color(255, 0, 0));
        }

        // Panel to hold event indicators
        JPanel eventsPanel = new JPanel();
        eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));

        // Filter events for this day
        java.util.List<CalendarEvent> dayEvents = events.stream()
                .filter(e -> e.getDateTime().toLocalDate().equals(date))
                .sorted(Comparator.comparing(CalendarEvent::getDateTime))
                .collect(Collectors.toList());

        // Add event indicators (limited to first 3 for space)
        int displayLimit = 3;
        for (int i = 0; i < Math.min(displayLimit, dayEvents.size()); i++) {
            CalendarEvent event = dayEvents.get(i);
            JPanel eventIndicator = new JPanel();
            eventIndicator.setPreferredSize(new Dimension(10, 12));

            // Use color based on category
            int colorIndex = Math.abs(event.getCategory().hashCode()) % categoryColors.length;
            eventIndicator.setBackground(categoryColors[colorIndex]);

            // Tooltip with event details
            String timeStr = event.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            eventIndicator.setToolTipText(timeStr + " - " + event.getTitle());

            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setPreferredSize(new Dimension(dayPanel.getWidth() - 6, 14));
            wrapperPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));

            JLabel eventLabel = new JLabel(timeStr + " " + truncateText(event.getTitle(), 15));
            eventLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));

            wrapperPanel.add(eventIndicator, BorderLayout.WEST);
            wrapperPanel.add(eventLabel, BorderLayout.CENTER);

            eventsPanel.add(wrapperPanel);
        }

        // Add indicator for more events if needed
        if (dayEvents.size() > displayLimit) {
            JLabel moreLabel = new JLabel("+" + (dayEvents.size() - displayLimit) + " more...");
            moreLabel.setFont(new Font("SansSerif", Font.ITALIC, 9));
            moreLabel.setForeground(Color.GRAY);
            eventsPanel.add(moreLabel);
        }

        dayPanel.add(dayLabel, BorderLayout.NORTH);
        dayPanel.add(eventsPanel, BorderLayout.CENTER);

        // Add click listener to add event on this day
        dayPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    // Show events for this day
                    if (!dayEvents.isEmpty()) {
                        showDayEventsDialog(date, dayEvents);
                    }
                } else if (e.getClickCount() == 2) {
                    // Add new event on this day
                    LocalDateTime dateTime = date.atTime(LocalTime.now().truncatedTo(ChronoUnit.HOURS));
                    CalendarEvent newEvent = new CalendarEvent("", dateTime);
                    controller.showEventDialog(newEvent);
                }
            }
        });

        return dayPanel;
    }

    private void showDayEventsDialog(LocalDate date, List<CalendarEvent> dayEvents) {
        JDialog dialog = new JDialog(frame, date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), true);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (CalendarEvent event : dayEvents) {
            String timeStr = event.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            listModel.addElement(timeStr + " - " + event.getTitle());
        }

        JList<String> eventsList = new JList<>(listModel);
        eventsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(eventsList);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add New");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);

        addButton.addActionListener(e -> {
            LocalDateTime dateTime = date.atTime(LocalTime.now().truncatedTo(ChronoUnit.HOURS));
            CalendarEvent newEvent = new CalendarEvent("", dateTime);
            dialog.dispose();
            controller.showEventDialog(newEvent);
        });

        editButton.addActionListener(e -> {
            int selectedIndex = eventsList.getSelectedIndex();
            if (selectedIndex >= 0) {
                CalendarEvent event = dayEvents.get(selectedIndex);
                dialog.dispose();
                controller.showEventDialog(event);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedIndex = eventsList.getSelectedIndex();
            if (selectedIndex >= 0) {
                CalendarEvent event = dayEvents.get(selectedIndex);
                int result = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to delete this event?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    controller.deleteEvent(controller.getEvents().indexOf(event));
                    dialog.dispose();
                }
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private JPanel createEventListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        // Filter panel
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        // Search field
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search events...");
        JButton searchButton = new JButton("Search");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Date filters
        JPanel dateFilterPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        dateFilterPanel.setBorder(BorderFactory.createTitledBorder("Date Range"));

        // Date pickers
        UtilDateModel startModel = new UtilDateModel();
        Properties startProps = new Properties();
        startProps.put("text.today", "Today");
        JDatePanelImpl startDatePanel = new JDatePanelImpl(startModel, startProps);
        startDatePicker = new JDatePickerImpl(startDatePanel, new DateLabelFormatter());

        UtilDateModel endModel = new UtilDateModel();
        Properties endProps = new Properties();
        endProps.put("text.today", "Today");
        JDatePanelImpl endDatePanel = new JDatePanelImpl(endModel, endProps);
        endDatePicker = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());

        dateFilterPanel.add(new JLabel("Start Date:"));
        dateFilterPanel.add(startDatePicker);
        dateFilterPanel.add(new JLabel("End Date:"));
        dateFilterPanel.add(endDatePicker);

        // Category filter
        JPanel categoryPanel = new JPanel(new BorderLayout());
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Category"));
        categoryFilterBox = new JComboBox<>(new String[]{"All Categories", "Work", "Personal", "Family", "Other"});
        categoryPanel.add(categoryFilterBox, BorderLayout.CENTER);

        // Show past events
        JPanel pastEventsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showPastEventsCheckbox = new JCheckBox("Show Past Events", true);
        pastEventsPanel.add(showPastEventsCheckbox);

        // Apply filters button
        JButton applyFiltersButton = new JButton("Apply Filters");
        JButton clearFiltersButton = new JButton("Clear Filters");
        JPanel filterButtonsPanel = new JPanel();
        filterButtonsPanel.add(applyFiltersButton);
        filterButtonsPanel.add(clearFiltersButton);

        // Add all filter components
        filterPanel.add(searchPanel);
        filterPanel.add(Box.createVerticalStrut(10));
        filterPanel.add(dateFilterPanel);
        filterPanel.add(Box.createVerticalStrut(10));
        filterPanel.add(categoryPanel);
        filterPanel.add(Box.createVerticalStrut(5));
        filterPanel.add(pastEventsPanel);
        // Continue the createEventListPanel() method
        filterPanel.add(Box.createVerticalStrut(10));
        filterPanel.add(filterButtonsPanel);

// Event table
        String[] columnNames = {"Time", "Title", "Category", "Priority"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        eventTable = new JTable(tableModel);
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setRowHeight(25);

// Set column widths
        TableColumnModel columnModel = eventTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(250);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(80);

// Custom renderer for category colors
        eventTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null && !isSelected) {
                    String category = value.toString();
                    int colorIndex = Math.abs(category.hashCode()) % categoryColors.length;
                    c.setBackground(categoryColors[colorIndex]);
                } else if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                } else {
                    c.setBackground(table.getBackground());
                }

                return c;
            }
        });

// Custom renderer for priority
        eventTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null && !isSelected) {
                    int priority = (Integer) value;
                    if (priority >= 8) {
                        c.setBackground(new Color(255, 200, 200)); // High priority - light red
                    } else if (priority >= 4) {
                        c.setBackground(new Color(255, 255, 200)); // Medium priority - light yellow
                    } else {
                        c.setBackground(new Color(200, 255, 200)); // Low priority - light green
                    }
                } else if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                } else {
                    c.setBackground(table.getBackground());
                }

                return c;
            }
        });

// Add sorting capability
        eventTable.setAutoCreateRowSorter(true);

// Event detail buttons
        JPanel eventButtonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton detailsButton = new JButton("Details");

        eventButtonPanel.add(addButton);
        eventButtonPanel.add(editButton);
        eventButtonPanel.add(deleteButton);
        eventButtonPanel.add(detailsButton);

// Add components to panel
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(eventTable), BorderLayout.CENTER);
        panel.add(eventButtonPanel, BorderLayout.SOUTH);

// Event listeners
        applyFiltersButton.addActionListener(e -> {
            LocalDate startDate = null;
            LocalDate endDate = null;

            if (startDatePicker.getModel().getValue() != null) {
                startDate = ((Date) startDatePicker.getModel().getValue()).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }

            if (endDatePicker.getModel().getValue() != null) {
                endDate = ((Date) endDatePicker.getModel().getValue()).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }

            String categoryFilter = categoryFilterBox.getSelectedItem().toString();
            String searchTerm = searchField.getText();

            if (!"All Categories".equals(categoryFilter)) {
                if (!searchTerm.isEmpty()) {
                    searchTerm += " category:" + categoryFilter;
                } else {
                    searchTerm = "category:" + categoryFilter;
                }
            }

            controller.filterEvents(searchTerm, startDate, endDate, showPastEventsCheckbox.isSelected());
        });

        clearFiltersButton.addActionListener(e -> {
            searchField.setText("");
            startDatePicker.getModel().setValue(null);
            endDatePicker.getModel().setValue(null);
            categoryFilterBox.setSelectedIndex(0);
            showPastEventsCheckbox.setSelected(true);
            controller.filterEvents("", null, null, true);
        });

// Table row selection listener
        eventTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = eventTable.getSelectedRow() != -1;
                editButton.setEnabled(rowSelected);
                deleteButton.setEnabled(rowSelected);
                detailsButton.setEnabled(rowSelected);
            }
        });

// Double-click to show details
        eventTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = eventTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        controller.showEventDetails(eventTable.convertRowIndexToModel(selectedRow));
                    }
                }
            }
        });

// Button action listeners
        addButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
            CalendarEvent newEvent = new CalendarEvent("", now);
            controller.showEventDialog(newEvent);
        });

        editButton.addActionListener(e -> {
            int selectedRow = eventTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = eventTable.convertRowIndexToModel(selectedRow);
                CalendarEvent event = controller.getFilteredAndSortedEvents().get(modelRow);
                controller.showEventDialog(event);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = eventTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = eventTable.convertRowIndexToModel(selectedRow);
                int result = JOptionPane.showConfirmDialog(
                        frame,
                        "Are you sure you want to delete this event?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION
                );
                if (result == JOptionPane.YES_OPTION) {
                    controller.deleteEvent(modelRow);
                }
            }
        });

        detailsButton.addActionListener(e -> {
            int selectedRow = eventTable.getSelectedRow();
            if (selectedRow >= 0) {
                controller.showEventDetails(eventTable.convertRowIndexToModel(selectedRow));
            }
        });

// Initialize button states
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        detailsButton.setEnabled(false);

        return panel;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");
        JButton importButton = new JButton("Import CSV");
        JButton exportButton = new JButton("Export CSV");
        JButton helpButton = new JButton("Help");

        toolbar.add(saveButton);
        toolbar.add(loadButton);
        toolbar.addSeparator();
        toolbar.add(importButton);
        toolbar.add(exportButton);
        toolbar.addSeparator();
        toolbar.add(helpButton);

        saveButton.addActionListener(e -> controller.saveEvents());

        loadButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    frame,
                    "Loading events will discard any unsaved changes. Continue?",
                    "Confirm Load",
                    JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
                controller.loadEvents();
            }
        });

        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                controller.importEvents(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".csv")) {
                    path += ".csv";
                }
                controller.exportEvents(path);
            }
        });

        helpButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    frame,
                    "Enhanced Calendar Application\n\n" +
                            "• Double-click on a day to add a new event\n" +
                            "• Single-click on a day to view events for that day\n" +
                            "• Use filters to narrow down events\n" +
                            "• Import/Export allows you to share your calendar with others\n" +
                            "• Calendar auto-saves every 5 minutes\n\n" +
                            "For more help, please refer to the user manual.",
                    "Help",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        return toolbar;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JLabel statusLabel = new JLabel("Ready");
        JLabel eventCountLabel = new JLabel("No events");

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(eventCountLabel, BorderLayout.EAST);

        return statusBar;
    }

    public void updateEventTable(List<CalendarEvent> events) {
        tableModel.setRowCount(0);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (CalendarEvent event : events) {
            Object[] row = {
                    event.getDateTime().format(timeFormatter),
                    event.getTitle(),
                    event.getCategory(),
                    event.getPriority()
            };
            tableModel.addRow(row);
        }

        // Update event count in status bar
        Component statusBar = frame.getContentPane().getComponent(2);
        if (statusBar instanceof JPanel) {
            Component eventCountLabel = ((JPanel) statusBar).getComponent(1);
            if (eventCountLabel instanceof JLabel) {
                String text = events.size() + " event" + (events.size() != 1 ? "s" : "");
                ((JLabel) eventCountLabel).setText(text);
            }
        }
    }

    public void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(frame, message, title, messageType);
    }

    public void showNotification(CalendarEvent event) {
        JDialog dialog = new JDialog(frame, "Event Reminder", false);
        dialog.setLayout(new BorderLayout());

        String title = event.getTitle();
        String time = event.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String date = event.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel timeLabel = new JLabel("Time: " + time, SwingConstants.CENTER);
        JLabel dateLabel = new JLabel("Date: " + date, SwingConstants.CENTER);

        JPanel detailsPanel = new JPanel(new GridLayout(3, 1));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        detailsPanel.add(titleLabel);
        detailsPanel.add(timeLabel);
        detailsPanel.add(dateLabel);

        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(e -> dialog.dispose());

        dialog.add(detailsPanel, BorderLayout.CENTER);
        dialog.add(dismissButton, BorderLayout.SOUTH);

        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);

        // Auto-close after 1 minute
        Timer timer = new Timer(60000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    public void showEventDialog(CalendarEvent eventToEdit) {
        boolean isNewEvent = (eventToEdit.getTitle().isEmpty());

        JDialog dialog = new JDialog(frame,
                isNewEvent ? "Add New Event" : "Edit Event",
                true);
        dialog.setLayout(new BorderLayout());

        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Title:"), gbc);

        JTextField titleField = new JTextField(eventToEdit.getTitle(), 30);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(titleField, gbc);

        // Date & Time
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Date:"), gbc);

        LocalDate eventDate = eventToEdit.getDateTime().toLocalDate();
        UtilDateModel dateModel = new UtilDateModel();
        dateModel.setDate(
                eventDate.getYear(),
                eventDate.getMonthValue() - 1,  // Java calendar months are 0-based
                eventDate.getDayOfMonth()
        );
        dateModel.setSelected(true);
        JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, new Properties());
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

        gbc.gridx = 1;
        formPanel.add(datePicker, gbc);

        // Time
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Time:"), gbc);

        LocalTime eventTime = eventToEdit.getDateTime().toLocalTime();
        SpinnerDateModel timeModel = new SpinnerDateModel();
        JSpinner timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, eventTime.getHour());
        calendar.set(Calendar.MINUTE, eventTime.getMinute());
        timeModel.setValue(calendar.getTime());

        gbc.gridx = 1;
        formPanel.add(timeSpinner, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Description:"), gbc);

        JTextArea descriptionArea = new JTextArea(eventToEdit.getDescription(), 5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 2;
        formPanel.add(descScrollPane, gbc);

        // Location
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        formPanel.add(new JLabel("Location:"), gbc);

        JTextField locationField = new JTextField(eventToEdit.getLocation(), 30);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(locationField, gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Category:"), gbc);

        String[] categories = {"Work", "Personal", "Family", "Other"};
        JComboBox<String> categoryBox = new JComboBox<>(categories);

        if (isNewEvent) {
            categoryBox.setSelectedIndex(0);
        } else {
            boolean found = false;
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(eventToEdit.getCategory())) {
                    categoryBox.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                categoryBox.setSelectedItem("Other");
            }
        }

        gbc.gridx = 1;
        formPanel.add(categoryBox, gbc);

        // Priority
        gbc.gridx = 0;
        gbc.gridy = 7;
        formPanel.add(new JLabel("Priority:"), gbc);

        SpinnerNumberModel priorityModel = new SpinnerNumberModel(
                isNewEvent ? 5 : eventToEdit.getPriority(),
                1, 10, 1);
        JSpinner prioritySpinner = new JSpinner(priorityModel);

        gbc.gridx = 1;
        formPanel.add(prioritySpinner, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        saveButton.addActionListener(e -> {
            // Validate inputs
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Title cannot be empty",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Get date from picker
                Date selectedDate = (Date) datePicker.getModel().getValue();
                LocalDate localDate = selectedDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                // Get time from spinner
                Date selectedTime = (Date) timeSpinner.getValue();
                Calendar cal = Calendar.getInstance();
                cal.setTime(selectedTime);
                LocalTime localTime = LocalTime.of(
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE)
                );

                // Combine into LocalDateTime
                LocalDateTime dateTime = LocalDateTime.of(localDate, localTime);

                // Create or update event
                if (isNewEvent) {
                    CalendarEvent newEvent = new CalendarEvent(
                            titleField.getText().trim(),
                            dateTime
                    );
                    newEvent.setDescription(descriptionArea.getText().trim());
                    newEvent.setLocation(locationField.getText().trim());
                    newEvent.setCategory(categoryBox.getSelectedItem().toString());
                    newEvent.setPriority((Integer) prioritySpinner.getValue());

                    controller.addEvent(newEvent);
                } else {
                    eventToEdit.setTitle(titleField.getText().trim());
                    eventToEdit.setDateTime(dateTime);
                    eventToEdit.setDescription(descriptionArea.getText().trim());
                    eventToEdit.setLocation(locationField.getText().trim());
                    eventToEdit.setCategory(categoryBox.getSelectedItem().toString());
                    eventToEdit.setPriority((Integer) prioritySpinner.getValue());

                    // Find the index of this event in the controller's list
                    int index = controller.getEvents().indexOf(eventToEdit);
                    if (index >= 0) {
                        controller.updateEvent(index, eventToEdit);
                    } else {
                        controller.addEvent(eventToEdit);
                    }
                }

                dialog.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Error processing date/time: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    public void showEventDetailsDialog(CalendarEvent event) {
        JDialog dialog = new JDialog(frame, "Event Details", true);
        dialog.setLayout(new BorderLayout());

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        detailsPanel.add(new JLabel("Title:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel(event.getTitle());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        detailsPanel.add(titleLabel, gbc);

        // Date & Time
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        detailsPanel.add(new JLabel("Date & Time:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        String formattedDateTime = event.getDateTime().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' hh:mm a")
        );
        detailsPanel.add(new JLabel(formattedDateTime), gbc);

        // Location
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        detailsPanel.add(new JLabel("Location:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        detailsPanel.add(new JLabel(event.getLocation()), gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        detailsPanel.add(new JLabel("Category:"), gbc);

        gbc.gridx = 1;
        JLabel categoryLabel = new JLabel(event.getCategory());
        int colorIndex = Math.abs(event.getCategory().hashCode()) % categoryColors.length;
        categoryLabel.setOpaque(true);
        categoryLabel.setBackground(categoryColors[colorIndex]);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        detailsPanel.add(categoryLabel, gbc);

        // Priority
        gbc.gridx = 0;
        gbc.gridy = 4;
        detailsPanel.add(new JLabel("Priority:"), gbc);

        gbc.gridx = 1;
        JLabel priorityLabel = new JLabel(String.valueOf(event.getPriority()) + " / 10");
        detailsPanel.add(priorityLabel, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 5;
        detailsPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;

        JTextArea descriptionArea = new JTextArea(event.getDescription());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(detailsPanel.getBackground());
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        scrollPane.setPreferredSize(new Dimension(300, 150));
        detailsPanel.add(scrollPane, gbc);

        // Time until event
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        detailsPanel.add(new JLabel("Status:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;

        String statusText;
        LocalDateTime now = LocalDateTime.now();
        if (event.getDateTime().isBefore(now)) {
            statusText = "Event has passed";
        } else {
            long days = ChronoUnit.DAYS.between(now.toLocalDate(), event.getDateTime().toLocalDate());
            if (days == 0) {
                // Today
                long hours = ChronoUnit.HOURS.between(now, event.getDateTime());
                if (hours == 0) {
                    long minutes = ChronoUnit.MINUTES.between(now, event.getDateTime());
                    statusText = "Starting in " + minutes + " minute" + (minutes != 1 ? "s" : "");
                } else {
                    statusText = "Today in " + hours + " hour" + (hours != 1 ? "s" : "");
                }
            } else if (days == 1) {
                statusText = "Tomorrow";
            } else {
                statusText = "In " + days + " days";
            }
        }

        JLabel statusLabel = new JLabel(statusText);
        detailsPanel.add(statusLabel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton editButton = new JButton("Edit");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(editButton);
        buttonPanel.add(closeButton);

        editButton.addActionListener(e -> {
            dialog.dispose();
            controller.showEventDialog(event);
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.add(detailsPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    // Date formatter for JDatePicker
    private class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (text == null || text.isEmpty()) {
                return null;
            }
            try {
                LocalDate date = LocalDate.parse(text, dateFormatter);
                return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException e) {
                throw new ParseException("Invalid date format", 0);
            }
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            }
            if (value instanceof Date) {
                LocalDate date = ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return dateFormatter.format(date);
            }
            return value.toString();
        }
    }
}

