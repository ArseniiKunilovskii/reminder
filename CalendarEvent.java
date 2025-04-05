import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event class representing calendar events
 */
class CalendarEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String description = "";
    private LocalDateTime dateTime;
    private String location = "";
    private String category = "Work";
    private int priority = 5; // 1-10 scale
    private boolean notified = false;

    public CalendarEvent(String title, LocalDateTime dateTime) {
        this.title = title;
        this.dateTime = dateTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }
} // End of class