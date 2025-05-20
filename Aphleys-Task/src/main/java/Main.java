
import java.util.*;
import java.io.*;
import java.text.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<String[]> transactions = new ArrayList<>();

        while (true) {
            System.out.println("\n1. Add Income");
            System.out.println("2. Add Expense");
            System.out.println("3. Load Transactions from CSV File");
            System.out.println("4. View Monthly Summary");
            System.out.println("5. Export Summary to CSV");
            System.out.println("6. Exit");
            System.out.print("Choose option: ");
            int choice = 0;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                System.out.println("Invalid input. Try again.");
                continue;
            }

            if (choice == 6) break;

            if (choice == 5) {
                try {
                    Map<String, Map<String, Double>> monthlyData = new TreeMap<>();
                    
                    // Collect data for each month
                    for (String[] t : transactions) {
                        String month = t[3].substring(0, 7); // Get YYYY-MM
                        double amount = Double.parseDouble(t[2]);
                        String type = t[0];
                        
                        monthlyData.putIfAbsent(month, new HashMap<>());
                        Map<String, Double> monthSummary = monthlyData.get(month);
                        
                        // Initialize if not present
                        monthSummary.putIfAbsent("Income", 0.0);
                        monthSummary.putIfAbsent("Expense", 0.0);
                        monthSummary.putIfAbsent("Net", 0.0);
                        
                        // Update amounts
                        if (type.equals("income")) {
                            monthSummary.put("Income", monthSummary.get("Income") + amount);
                        } else {
                            monthSummary.put("Expense", monthSummary.get("Expense") + amount);
                        }
                        monthSummary.put("Net", monthSummary.get("Income") - monthSummary.get("Expense"));
                    }
                    
                    // Write to CSV
                    try (FileWriter fw = new FileWriter("monthly_summary.csv")) {
                        fw.write("Month,Category,Income,Expense,Net Balance\n");
                        for (Map.Entry<String, Map<String, Double>> entry : monthlyData.entrySet()) {
                            String month = entry.getKey();
                            Map<String, Double> categoryIncomes = new HashMap<>();
                            Map<String, Double> categoryExpenses = new HashMap<>();
                            
                            // Calculate category-wise totals
                            for (String[] t : transactions) {
                                if (t[3].startsWith(month)) {
                                    String category = t[1];
                                    double amount = Double.parseDouble(t[2]);
                                    if (t[0].equals("income")) {
                                        categoryIncomes.put(category, categoryIncomes.getOrDefault(category, 0.0) + amount);
                                    } else {
                                        categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + amount);
                                    }
                                }
                            }
                            
                            // Write category-wise breakdown
                            Set<String> allCategories = new HashSet<>();
                            allCategories.addAll(categoryIncomes.keySet());
                            allCategories.addAll(categoryExpenses.keySet());
                            
                            for (String category : allCategories) {
                                double income = categoryIncomes.getOrDefault(category, 0.0);
                                double expense = categoryExpenses.getOrDefault(category, 0.0);
                                fw.write(String.format("%s,%s,%.2f,%.2f,%.2f\n",
                                    month,
                                    category,
                                    income,
                                    expense,
                                    income - expense));
                            }
                            
                            // Write monthly totals
                            Map<String, Double> summary = entry.getValue();
                            fw.write(String.format("%s,TOTAL,%.2f,%.2f,%.2f\n",
                                month,
                                summary.get("Income"),
                                summary.get("Expense"),
                                summary.get("Net")));
                            fw.write("\n"); // Add blank line between months
                        }
                    }
                    System.out.println("Summary exported to monthly_summary.csv");
                } catch (IOException e) {
                    System.out.println("Error exporting summary: " + e.getMessage());
                }
                continue;
            }

            if (choice == 1 || choice == 2) {
                String type = (choice == 1) ? "income" : "expense";
                String categories = (choice == 1) ? "salary/business" : "food/rent/travel";
                System.out.print("Enter category (" + categories + "): ");
                String category = scanner.nextLine();

                System.out.print("Enter amount: ");
                double amount = 0;
                try {
                    amount = Double.parseDouble(scanner.nextLine());
                } catch (Exception e) {
                    System.out.println("Invalid amount. Try again.");
                    continue;
                }

                System.out.print("Enter date (DD-MM-YYYY): ");
                String dateInput = scanner.nextLine();
                String date = normalizeDate(dateInput);
                if (date == null) {
                    System.out.println("Invalid date format. Try again.");
                    continue;
                }

                transactions.add(new String[]{type, category, String.valueOf(amount), date});
                System.out.println("Transaction added!");
            } else if (choice == 3) {
                System.out.print("Enter the path to your CSV file: ");
                String filePath = scanner.nextLine();
                
                int count = loadFromCSV(filePath, transactions);
                if (count >= 0) {
                    System.out.println("Loaded " + count + " transactions from file.");
                }
            } else if (choice == 4) {
                System.out.print("Enter month to view summary (YYYY-MM): ");
                String month = scanner.nextLine();

                double totalIncome = 0, totalExpense = 0;
                Map<String, Double> categoryTotals = new HashMap<>();

                for (String[] t : transactions) {
                    if (t[3].startsWith(month)) {
                        double amt = Double.parseDouble(t[2]);
                        if (t[0].equals("income")) {
                            totalIncome += amt;
                            categoryTotals.put(t[1], categoryTotals.getOrDefault(t[1], 0.0) + amt);
                        } else {
                            totalExpense += amt;
                            categoryTotals.put(t[1], categoryTotals.getOrDefault(t[1], 0.0) - amt); // Store expenses as negative
                        }
                    }
                }

                System.out.println("\nSummary for " + month + ":");
                for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                    System.out.printf("%-10s: %.2f\n", entry.getKey(), entry.getValue());
                }
                System.out.printf("Total Income : %.2f\n", totalIncome);
                System.out.printf("Total Expense: %.2f\n", totalExpense);
                System.out.printf("Net Balance  : %.2f\n", totalIncome - totalExpense);
            } else {
                System.out.println("Invalid option. Try again.");
            }
        }
        scanner.close();
        System.out.println("Thank you for using the Expense Tracker!");
    }

    // Loads transactions from a CSV file and adds them to the list
    public static int loadFromCSV(String filename, List<String[]> transactions) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String type = parts[0].trim().toLowerCase();
                    String category = parts[1].trim();
                    double amount = Double.parseDouble(parts[2].trim());
                    String date = normalizeDate(parts[3].trim());
                    if (date == null) {
                        System.out.println("Skipping entry with invalid date: " + parts[3]);
                        continue;
                    }
                    if ((type.equals("income") || type.equals("expense")) && !category.isEmpty() && !date.isEmpty()) {
                        transactions.add(new String[]{type, category, String.valueOf(amount), date});
                        count++;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            return -1;
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            return -1;
        }
        return count;
    }

    // Accepts date in DD-MM-YYYY format and returns as YYYY-MM-DD
    public static String normalizeDate(String input) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
            inputFormat.setLenient(false);
            Date date = inputFormat.parse(input);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            return outputFormat.format(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
