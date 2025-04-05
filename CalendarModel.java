import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

    /**
     * Model class that manages data and business logic
     */
    class CalendarModel {
        private List<CalendarEvent> events;
        private List<CalendarEvent> filteredEvents;
        private String searchText = "";
        private LocalDate filterStartDate = null;
        private LocalDate filterEndDate = null;
        private boolean showPastEvents = true;
        private LocalDate currentDisplayMonth = YearMonth.now().atDay(1);

        private static final String SAVE_FILE = "calendar_events.dat";
        private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public CalendarModel() {
            events = new ArrayList<>();
            filteredEvents = new ArrayList<>();
        }

        public void addEvent(CalendarEvent event) {
            events.add(event);
            applyFilters();
        }

        public void updateEvent(int index, CalendarEvent event) {
            if (index >= 0 && index < filteredEvents.size()) {
                int actualIndex = events.indexOf(filteredEvents.get(index));
                if (actualIndex >= 0) {
                    events.set(actualIndex, event);
                    applyFilters();
                }
            }
        }

        public void deleteEvent(int index) {
            if (index >= 0 && index < filteredEvents.size()) {
                events.remove(filteredEvents.get(index));
                applyFilters();
            }
        }

        public List<CalendarEvent> getEvents() {
            return events;
        }

        public List<CalendarEvent> getFilteredAndSortedEvents() {
            return filteredEvents;
        }

        public void setFilters(String searchText, LocalDate startDate, LocalDate endDate, boolean showPastEvents) {
            this.searchText = searchText.toLowerCase();
            this.filterStartDate = startDate;
            this.filterEndDate = endDate;
            this.showPastEvents = showPastEvents;
            applyFilters();
        }

        private void applyFilters() {
            filteredEvents = events.stream()
                    .filter(event -> {
                        // Filter by search text
                        if (!searchText.isEmpty() &&
                                !event.getTitle().toLowerCase().contains(searchText) &&
                                !event.getDescription().toLowerCase().contains(searchText)) {
                            return false;
                        }

                        // Filter by date range
                        if (filterStartDate != null &&
                                event.getDateTime().toLocalDate().isBefore(filterStartDate)) {
                            return false;
                        }

                        if (filterEndDate != null &&
                                event.getDateTime().toLocalDate().isAfter(filterEndDate)) {
                            return false;
                        }

                        // Filter past events if needed
                        if (!showPastEvents &&
                                event.getDateTime().isBefore(LocalDateTime.now())) {
                            return false;
                        }

                        return true;
                    })
                    .sorted(Comparator.comparing(CalendarEvent::getDateTime))
                    .collect(Collectors.toList());
        }

        public void saveEvents() {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
                out.writeObject(events);
            } catch (IOException e) {
                System.err.println("Error saving events: " + e.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        public void loadEvents() {
            File file = new File(SAVE_FILE);
            if (!file.exists()) {
                events = new ArrayList<>();
                return;
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
                events = (List<CalendarEvent>) in.readObject();
                applyFilters();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading events: " + e.getMessage());
                events = new ArrayList<>();
            }
        }

        public void exportEventsToCSV(String filePath) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("Title,Description,Date,Time,Location,Category,Priority");

                for (CalendarEvent event : events) {
                    String csvLine = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s",
                            escapeCSV(event.getTitle()),
                            escapeCSV(event.getDescription()),
                            event.getDateTime().format(CSV_DATE_FORMAT),
                            event.getLocation(),
                            event.getCategory(),
                            event.getPriority()
                    );
                    writer.println(csvLine);
                }
            } catch (IOException e) {
                System.err.println("Error exporting to CSV: " + e.getMessage());
            }
        }

        private String escapeCSV(String input) {
            if (input == null) return "";
            return input.replace("\"", "\"\"");
        }

        public void importEventsFromCSV(String filePath) throws IOException {
            List<CalendarEvent> importedEvents = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                // Skip header
                String line = reader.readLine();

                while ((line = reader.readLine()) != null) {
                    try {
                        String[] fields = parseCSVLine(line);
                        if (fields.length >= 7) {
                            String title = fields[0];
                            String description = fields[1];
                            LocalDateTime dateTime = LocalDateTime.parse(fields[2], CSV_DATE_FORMAT);
                            String location = fields[3];
                            String category = fields[4];
                            int priority = Integer.parseInt(fields[5]);

                            CalendarEvent event = new CalendarEvent(title, dateTime);
                            event.setDescription(description);
                            event.setLocation(location);
                            event.setCategory(category);
                            event.setPriority(priority);

                            importedEvents.add(event);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing CSV line: " + e.getMessage());
                    }
                }
            }

            events.addAll(importedEvents);
            applyFilters();
        }

        private String[] parseCSVLine(String line) {
            List<String> result = new ArrayList<>();
            boolean inQuotes = false;
            StringBuilder field = new StringBuilder();

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        // Escaped quote
                        field.append('\"');
                        i++; // Skip the next quote
                    } else {
                        // Toggle quote mode
                        inQuotes = !inQuotes;
                    }
                } else if (c == ',' && !inQuotes) {
                    // End of field
                    result.add(field.toString());
                    field = new StringBuilder();
                } else {
                    field.append(c);
                }
            }

            // Add the last field
            result.add(field.toString());

            return result.toArray(new String[0]);
        }

        public LocalDate getCurrentDisplayMonth() {
            return currentDisplayMonth;
        }

        public void setCurrentDisplayMonth(LocalDate date) {
            this.currentDisplayMonth = YearMonth.from(date).atDay(1);
        }
    }

