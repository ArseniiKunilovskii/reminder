import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;



/**
 * Enhanced Calendar Application with MVC architecture, better UI/UX, and additional features
 */
public class CalendarApp {
    public static void main(String[] args) {
        try {
            // Set system look and feel for better UI appearance
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            CalendarController controller = new CalendarController();
            controller.initialize();
        });
    }
}





