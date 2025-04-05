import javax.swing.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Controller class that manages interaction between model and view
 */
class CalendarController {
    private CalendarModel model;
    private CalendarView view;
    private ScheduledExecutorService notificationScheduler;
    private ScheduledExecutorService autoSaveScheduler;

    public void initialize() {
        model = new CalendarModel();
        view = new CalendarView(this);

        model.loadEvents();
        updateEventDisplay();

        startNotificationScheduler();
        startAutoSaveScheduler();
    }

    public void addEvent(CalendarEvent event) {
        model.addEvent(event);
        updateEventDisplay();
    }

    public void updateEvent(int index, CalendarEvent event) {
        model.updateEvent(index, event);
        updateEventDisplay();
    }

    public void deleteEvent(int index) {
        model.deleteEvent(index);
        updateEventDisplay();
    }

    public List<CalendarEvent> getEvents() {
        return model.getEvents();
    }

    public void saveEvents() {
        model.saveEvents();
        view.showMessage("Events saved successfully", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    public void loadEvents() {
        model.loadEvents();
        updateEventDisplay();
    }

    public void exportEvents(String filePath) {
        model.exportEventsToCSV(filePath);
        view.showMessage("Events exported to CSV successfully", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    public void importEvents(String filePath) {
        try {
            model.importEventsFromCSV(filePath);
            updateEventDisplay();
            view.showMessage("Events imported successfully", "Import Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            view.showMessage("Error importing events: " + e.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateEventDisplay() {
        view.updateEventTable(model.getFilteredAndSortedEvents());
        view.updateCalendarPanel(model.getEvents());
    }

    public void filterEvents(String searchText, LocalDate startDate, LocalDate endDate, boolean showPastEvents) {
        model.setFilters(searchText, startDate, endDate, showPastEvents);
        updateEventDisplay();
    }

    public void showEventDetails(int index) {
        if (index >= 0 && index < model.getFilteredAndSortedEvents().size()) {
            CalendarEvent event = model.getFilteredAndSortedEvents().get(index);
            view.showEventDetailsDialog(event);
        }
    }

    private void startNotificationScheduler() {
        if (notificationScheduler != null && !notificationScheduler.isShutdown()) {
            notificationScheduler.shutdown();
        }

        notificationScheduler = Executors.newScheduledThreadPool(1);
        notificationScheduler.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fiveMinutesFromNow = now.plusMinutes(5);

            List<CalendarEvent> upcomingEvents = model.getEvents().stream()
                    .filter(event -> {
                        // Check for events that are happening now or in 5 minutes
                        LocalDateTime eventTime = event.getDateTime();
                        return (eventTime.withSecond(0).withNano(0).equals(now.withSecond(0).withNano(0)) ||
                                (eventTime.isAfter(now) && eventTime.isBefore(fiveMinutesFromNow)));
                    })
                    .collect(Collectors.toList());

            for (CalendarEvent event : upcomingEvents) {
                // Only notify if it hasn't been notified already
                if (!event.isNotified()) {
                    event.setNotified(true);
                    SwingUtilities.invokeLater(() -> {
                        view.showNotification(event);
                    });
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void startAutoSaveScheduler() {
        if (autoSaveScheduler != null && !autoSaveScheduler.isShutdown()) {
            autoSaveScheduler.shutdown();
        }

        autoSaveScheduler = Executors.newScheduledThreadPool(1);
        autoSaveScheduler.scheduleAtFixedRate(() -> {
            model.saveEvents();
        }, 5, 5, TimeUnit.MINUTES);
    }

    public void shutdown() {
        if (notificationScheduler != null) {
            notificationScheduler.shutdown();
        }
        if (autoSaveScheduler != null) {
            autoSaveScheduler.shutdown();
        }
        model.saveEvents();
    }

    public void showEventDialog(CalendarEvent eventToEdit) {
        view.showEventDialog(eventToEdit);
    }

    public LocalDate getCurrentDisplayMonth() {
        return model.getCurrentDisplayMonth();
    }

    public void setCurrentDisplayMonth(LocalDate date) {
        model.setCurrentDisplayMonth(date);
        updateEventDisplay();
    }
}