import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.*;

// Task class represents one scheduled task
class Task {
    private final String description;
    private LocalTime startTime;
    private LocalTime endTime;
    private final String priority;
    private boolean completed;

    public Task(String description, LocalTime startTime, LocalTime endTime, String priority) {
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = priority;
        this.completed = false;
    }

    public String getDescription() {
        return description;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        String status = completed ? "[Completed]" : "";
        return String.format("%s - %s: %s [%s] %s",
                startTime.toString(),
                endTime.toString(),
                description,
                priority,
                status);
    }
}

// Factory Pattern to create Task instances
class TaskFactory {
    public static Task createTask(String description, String start, String end, String priority) throws IllegalArgumentException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime, endTime;
        try {
            startTime = LocalTime.parse(start, formatter);
            endTime = LocalTime.parse(end, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Error: Invalid time format.");
        }

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("Error: End time must be after start time.");
        }

        return new Task(description, startTime, endTime, priority);
    }
}

// Observer interface for task events
interface TaskObserver {
    void onTaskAdded(Task newTask);
    void onTaskRemoved(Task removedTask);
    void onTaskConflict(Task conflictTask, Task existingTask);
    void onTaskUpdated(Task updatedTask);
}

// Singleton Schedule Manager that holds tasks and notifies observers
class ScheduleManager {
    private static volatile ScheduleManager instance = null;
    private final List<Task> tasks;
    private final List<TaskObserver> observers;
    private final Logger logger;

    private ScheduleManager() {
        tasks = new ArrayList<>();
        observers = new ArrayList<>();
        logger = Logger.getLogger(ScheduleManager.class.getName());
        setupLogger();
    }

    public static ScheduleManager getInstance() {
        if (instance == null) {
            synchronized (ScheduleManager.class) {
                if (instance == null) {
                    instance = new ScheduleManager();
                }
            }
        }
        return instance;
    }

    private void setupLogger() {
        try {
            Handler fh = new FileHandler("schedule.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (Exception e) {
            System.err.println("Logger setup failed: " + e.getMessage());
        }
    }

    public void addObserver(TaskObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TaskObserver observer) {
        observers.remove(observer);
    }

    // Check overlap - returns conflicting task if overlap found else null
    private Task checkOverlap(Task newTask) {
        for (Task t : tasks) {
            if (t.getDescription().equalsIgnoreCase(newTask.getDescription())) {
                // Same description considered a conflict? No, only time overlap matters
                continue;
            }
            if (timesOverlap(t.getStartTime(), t.getEndTime(), newTask.getStartTime(), newTask.getEndTime())) {
                return t;
            }
        }
        return null;
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    public synchronized String addTask(Task task) {
        Task conflict = checkOverlap(task);
        if (conflict != null) {
            notifyConflict(task, conflict);
            logger.warning("Add Task failed due to conflict: " + task.getDescription() + " conflicts with " + conflict.getDescription());
            return "Error: Task conflicts with existing task \"" + conflict.getDescription() + "\".";
        }
        tasks.add(task);
        tasks.sort(Comparator.comparing(Task::getStartTime));
        notifyAdded(task);
        logger.info("Task added: " + task.getDescription());
        return "Task added successfully. No conflicts.";
    }

    public synchronized String removeTask(String description) {
        Task toRemove = null;
        for (Task t : tasks) {
            if (t.getDescription().equalsIgnoreCase(description)) {
                toRemove = t;
                break;
            }
        }
        if (toRemove == null) {
            logger.warning("Remove Task failed - not found: " + description);
            return "Error: Task not found.";
        }
        tasks.remove(toRemove);
        notifyRemoved(toRemove);
        logger.info("Task removed: " + description);
        return "Task removed successfully.";
    }

    public synchronized List<Task> viewTasks() {
        return new ArrayList<>(tasks);
    }

    public synchronized List<Task> viewTasksByPriority(String priority) {
        List<Task> filtered = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getPriority().equalsIgnoreCase(priority)) {
                filtered.add(t);
            }
        }
        filtered.sort(Comparator.comparing(Task::getStartTime));
        return filtered;
    }

    // Optional: Edit task times (description unique, so editing times based on description)
    public synchronized String editTask(String description, String newStart, String newEnd) {
        Task toEdit = null;
        for (Task t : tasks) {
            if (t.getDescription().equalsIgnoreCase(description)) {
                toEdit = t;
                break;
            }
        }
        if (toEdit == null) {
            return "Error: Task not found.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime, endTime;
        try {
            startTime = LocalTime.parse(newStart, formatter);
            endTime = LocalTime.parse(newEnd, formatter);
        } catch (DateTimeParseException e) {
            return "Error: Invalid time format.";
        }
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            return "Error: End time must be after start time.";
        }

        // Temporarily remove to check overlap excluding itself
        tasks.remove(toEdit);
        Task temp = new Task(description, startTime, endTime, toEdit.getPriority());
        Task conflict = checkOverlap(temp);
        if (conflict != null) {
            // Restore original and notify conflict
            tasks.add(toEdit);
            return "Error: Task conflicts with existing task \"" + conflict.getDescription() + "\".";
        }
        // Update times
        toEdit.setStartTime(startTime);
        toEdit.setEndTime(endTime);
        tasks.add(toEdit);
        tasks.sort(Comparator.comparing(Task::getStartTime));
        notifyUpdated(toEdit);
        return "Task updated successfully.";
    }

    public synchronized String markTaskCompleted(String description) {
        for (Task t : tasks) {
            if (t.getDescription().equalsIgnoreCase(description)) {
                t.markCompleted();
                notifyUpdated(t);
                return "Task marked as completed.";
            }
        }
        return "Error: Task not found.";
    }

    // Notify observers methods
    private void notifyAdded(Task task) {
        for (TaskObserver obs : observers) {
            obs.onTaskAdded(task);
        }
    }

    private void notifyRemoved(Task task) {
        for (TaskObserver obs : observers) {
            obs.onTaskRemoved(task);
        }
    }

    private void notifyConflict(Task newTask, Task existingTask) {
        for (TaskObserver obs : observers) {
            obs.onTaskConflict(newTask, existingTask);
        }
    }

    private void notifyUpdated(Task task) {
        for (TaskObserver obs : observers) {
            obs.onTaskUpdated(task);
        }
    }
}

// Simple Console Logger for task notifications (implements Observer)
class ConsoleTaskObserver implements TaskObserver {
    @Override
    public void onTaskAdded(Task newTask) {
        System.out.println("Notification: Task \"" + newTask.getDescription() + "\" added.");
    }

    @Override
    public void onTaskRemoved(Task removedTask) {
        System.out.println("Notification: Task \"" + removedTask.getDescription() + "\" removed.");
    }

    @Override
    public void onTaskConflict(Task conflictTask, Task existingTask) {
        System.out.println("Notification: Conflict detected! \"" + conflictTask.getDescription() +
                "\" conflicts with existing task \"" + existingTask.getDescription() + "\".");
    }

    @Override
    public void onTaskUpdated(Task updatedTask) {
        System.out.println("Notification: Task \"" + updatedTask.getDescription() + "\" updated.");
    }
}

// Main Application with console interface
public class AstronautScheduleOrganizer {
    private static final Scanner scanner = new Scanner(System.in);
    private static final ScheduleManager scheduleManager = ScheduleManager.getInstance();

    public static void main(String[] args) {
        scheduleManager.addObserver(new ConsoleTaskObserver());

        System.out.println("Welcome to Astronaut Daily Schedule Organizer");
        printHelp();

        while (true) {
            System.out.print("\nEnter command: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting. Goodbye!");
                break;
            }

            try {
                handleCommand(input);
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    private static void handleCommand(String input) {
        if (input.isEmpty()) {
            System.out.println("Please enter a command.");
            return;
        }

        String[] parts = input.split(" ", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "addtask" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: addTask \"Description\" HH:mm HH:mm Priority");
                    return;
                }
                addTask(parts[1]);
            }
            case "removetask" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: removeTask \"Description\"");
                    return;
                }
                removeTask(parts[1]);
            }
            case "viewtasks" -> viewTasks(null);
            case "viewpriority" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: viewPriority Priority");
                    return;
                }
                viewTasks(parts[1]);
            }
            case "edittask" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: editTask \"Description\" NewStartTime NewEndTime");
                    return;
                }
                editTask(parts[1]);
            }
            case "markcompleted" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: markCompleted \"Description\"");
                    return;
                }
                markCompleted(parts[1]);
            }
            case "help" -> printHelp();
            default -> System.out.println("Unknown command. Type 'help' for commands list.");
        }
    }

    private static void addTask(String args) {
        // Format: "Description" HH:mm HH:mm Priority
        try {
            List<String> tokens = parseQuotedArgs(args);
            if (tokens.size() != 4) {
                System.out.println("Invalid arguments. Usage: addTask \"Description\" HH:mm HH:mm Priority");
                return;
            }
            String description = tokens.get(0);
            String start = tokens.get(1);
            String end = tokens.get(2);
            String priority = tokens.get(3);

            Task task = TaskFactory.createTask(description, start, end, priority);
            String result = scheduleManager.addTask(task);
            System.out.println(result);

        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void removeTask(String args) {
        String description = extractQuoted(args);
        if (description == null) {
            System.out.println("Invalid argument. Usage: removeTask \"Description\"");
            return;
        }
        String result = scheduleManager.removeTask(description);
        System.out.println(result);
    }

    private static void viewTasks(String priority) {
        List<Task> tasks;
        if (priority == null) {
            tasks = scheduleManager.viewTasks();
        } else {
            tasks = scheduleManager.viewTasksByPriority(priority);
        }

        if (tasks.isEmpty()) {
            System.out.println("No tasks scheduled for the day.");
            return;
        }

        for (Task t : tasks) {
            System.out.println(t);
        }
    }

    private static void editTask(String args) {
        // Format: "Description" HH:mm HH:mm
        try {
            List<String> tokens = parseQuotedArgs(args);
            if (tokens.size() != 3) {
                System.out.println("Invalid arguments. Usage: editTask \"Description\" NewStartTime NewEndTime");
                return;
            }
            String description = tokens.get(0);
            String newStart = tokens.get(1);
            String newEnd = tokens.get(2);

            String result = scheduleManager.editTask(description, newStart, newEnd);
            System.out.println(result);

        } catch (Exception e) {
            System.out.println("Error editing task: " + e.getMessage());
        }
    }

    private static void markCompleted(String args) {
        String description = extractQuoted(args);
        if (description == null) {
            System.out.println("Invalid argument. Usage: markCompleted \"Description\"");
            return;
        }
        String result = scheduleManager.markTaskCompleted(description);
        System.out.println(result);
    }

    private static List<String> parseQuotedArgs(String input) {
        List<String> tokens = new ArrayList<>();
        int firstQuote = input.indexOf('"');
        int secondQuote = input.indexOf('"', firstQuote + 1);
        if (firstQuote == -1 || secondQuote == -1) return tokens;

        String desc = input.substring(firstQuote + 1, secondQuote);
        tokens.add(desc);

        String remainder = input.substring(secondQuote + 1).trim();
        // Expect remainder like: HH:mm HH:mm Priority
        String[] parts = remainder.split("\\s+");
        tokens.addAll(Arrays.asList(parts));
        return tokens;
    }

    private static String extractQuoted(String input) {
        int firstQuote = input.indexOf('"');
        int secondQuote = input.indexOf('"', firstQuote + 1);
        if (firstQuote == -1 || secondQuote == -1) return null;
        return input.substring(firstQuote + 1, secondQuote);
    }

    private static void printHelp() {
        System.out.println("""
                Commands:
                addTask "Description" HH:mm HH:mm Priority - Add a new task
                removeTask "Description" - Remove a task
                viewTasks - View all tasks sorted by start time
                viewPriority Priority - View tasks filtered by priority
                editTask "Description" NewStartTime NewEndTime - Edit an existing task's time
                markCompleted "Description" - Mark task as completed
                help - Show this help
                exit - Exit the program
                """);
    }
}
