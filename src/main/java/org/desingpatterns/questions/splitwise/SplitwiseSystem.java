package org.desingpatterns.questions.splitwise;

import java.util.*;
import java.util.stream.Collectors;

// User class
class User {
    private String id;
    private String name;
    private String email;

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}

// Expense class
class Expense {
    private String id;
    private String description;
    private double amount;
    private User payer;
    private List<User> participants;
    private Map<User, Double> shares;

    public Expense(String id, String description, double amount, User payer,
                   List<User> participants, Map<User, Double> shares) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.payer = payer;
        this.participants = participants;
        this.shares = shares;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public User getPayer() { return payer; }
    public List<User> getParticipants() { return participants; }
    public Map<User, Double> getShares() { return shares; }
}

// Transaction class
class Transaction {
    private User from;
    private User to;
    private double amount;

    public Transaction(User from, User to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public User getFrom() { return from; }
    public User getTo() { return to; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return from.getName() + " pays " + to.getName() + " $" + String.format("%.2f", amount);
    }
}

// UserPair class for balance tracking
class UserPair {
    private User user1;
    private User user2;

    public UserPair(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    public User getUser1() { return user1; }
    public User getUser2() { return user2; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPair)) return false;
        UserPair userPair = (UserPair) o;
        return (user1.equals(userPair.user1) && user2.equals(userPair.user2)) ||
                (user1.equals(userPair.user2) && user2.equals(userPair.user1));
    }

    @Override
    public int hashCode() {
        // Order doesn't matter for hash code
        return user1.hashCode() + user2.hashCode();
    }
}

// Split interface
interface Split {
    Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails);
}

// Equal split implementation
class EqualSplit implements Split {
    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        double amountPerPerson = amount / participants.size();
        Map<User, Double> splits = new HashMap<>();
        for (User user : participants) {
            splits.put(user, amountPerPerson);
        }
        return splits;
    }
}

// Percentage split implementation
class PercentageSplit implements Split {
    @Override
    public Map<User, Double> calculateSplit(double amount, List<User> participants, Map<String, Object> splitDetails) {
        Map<User, Double> percentages = (Map<User, Double>) splitDetails.get("percentages");
        Map<User, Double> splits = new HashMap<>();

        for (User user : participants) {
            double percentage = percentages.getOrDefault(user, 0.0);
            splits.put(user, amount * percentage / 100.0);
        }
        return splits;
    }
}

// Split factory
class SplitFactory {
    public static Split createSplit(String splitType) {
        switch (splitType.toUpperCase()) {
            case "EQUAL":
                return new EqualSplit();
            case "PERCENTAGE":
                return new PercentageSplit();
            default:
                throw new IllegalArgumentException("Unknown split type: " + splitType);
        }
    }
}

// Observer interface
interface ExpenseObserver {
    void onExpenseAdded(Expense expense);
    void onExpenseUpdated(Expense expense);
}

// Subject interface
interface ExpenseSubject {
    void addObserver(ExpenseObserver observer);
    void removeObserver(ExpenseObserver observer);
    void notifyExpenseAdded(Expense expense);
    void notifyExpenseUpdated(Expense expense);
}

// Expense manager implementation
class ExpenseManager implements ExpenseSubject {
    private List<ExpenseObserver> observers = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();

    @Override
    public void addObserver(ExpenseObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ExpenseObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyExpenseAdded(Expense expense) {
        for (ExpenseObserver observer : observers) {
            observer.onExpenseAdded(expense);
        }
    }

    @Override
    public void notifyExpenseUpdated(Expense expense) {
        for (ExpenseObserver observer : observers) {
            observer.onExpenseUpdated(expense);
        }
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        notifyExpenseAdded(expense);
    }

    public void updateExpense(Expense expense) {
        for (int i = 0; i < expenses.size(); i++) {
            if (expenses.get(i).getId().equals(expense.getId())) {
                expenses.set(i, expense);
                notifyExpenseUpdated(expense);
                return;
            }
        }
        throw new IllegalArgumentException("Expense with ID " + expense.getId() + " not found.");
    }

    public List<Expense> getAllExpenses() {
        return new ArrayList<>(expenses);
    }
}

// Balance sheet implementation
class BalanceSheet implements ExpenseObserver {
    private Map<UserPair, Double> balances = new HashMap<>();

    @Override
    public void onExpenseAdded(Expense expense) {
        updateBalances(expense);
    }

    @Override
    public void onExpenseUpdated(Expense expense) {
        updateBalances(expense);
    }

    private void updateBalances(Expense expense) {
        User payer = expense.getPayer();
        Map<User, Double> shares = expense.getShares();

        for (Map.Entry<User, Double> entry : shares.entrySet()) {
            User participant = entry.getKey();
            Double amount = entry.getValue();

            if (!participant.equals(payer)) {
                UserPair userPair = new UserPair(participant, payer);
                Double currentBalance = balances.getOrDefault(userPair, 0.0);
                balances.put(userPair, currentBalance + amount);
            }
        }
    }

    public double getBalance(User user1, User user2) {
        UserPair pair1 = new UserPair(user1, user2);
        UserPair pair2 = new UserPair(user2, user1);

        double balance1 = balances.getOrDefault(pair1, 0.0);
        double balance2 = balances.getOrDefault(pair2, 0.0);

        return balance1 - balance2;
    }

    public double getTotalBalance(User user) {
        double total = 0.0;
        for (Map.Entry<UserPair, Double> entry : balances.entrySet()) {
            UserPair pair = entry.getKey();
            double amount = entry.getValue();

            if (pair.getUser1().equals(user)) {
                total -= amount;
            } else if (pair.getUser2().equals(user)) {
                total += amount;
            }
        }
        return total;
    }

    public List<Transaction> getSimplifiedSettlements() {
        Map<User, Double> netBalances = new HashMap<>();

        for (Map.Entry<UserPair, Double> entry : balances.entrySet()) {
            UserPair pair = entry.getKey();
            double amount = entry.getValue();

            User debtor = pair.getUser1();
            User creditor = pair.getUser2();

            netBalances.put(debtor, netBalances.getOrDefault(debtor, 0.0) - amount);
            netBalances.put(creditor, netBalances.getOrDefault(creditor, 0.0) + amount);
        }

        List<User> debtors = new ArrayList<>();
        List<User> creditors = new ArrayList<>();

        for (Map.Entry<User, Double> entry : netBalances.entrySet()) {
            User user = entry.getKey();
            double balance = entry.getValue();

            if (balance < -0.001) {
                debtors.add(user);
            } else if (balance > 0.001) {
                creditors.add(user);
            }
        }

        debtors.sort((a, b) -> Double.compare(netBalances.get(a), netBalances.get(b)));
        creditors.sort((a, b) -> Double.compare(netBalances.get(b), netBalances.get(a)));

        List<Transaction> transactions = new ArrayList<>();
        int debtorIndex = 0;
        int creditorIndex = 0;

        while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
            User debtor = debtors.get(debtorIndex);
            User creditor = creditors.get(creditorIndex);

            double debtorBalance = netBalances.get(debtor);
            double creditorBalance = netBalances.get(creditor);

            double transferAmount = Math.min(Math.abs(debtorBalance), creditorBalance);

            transactions.add(new Transaction(debtor, creditor, transferAmount));

            netBalances.put(debtor, debtorBalance + transferAmount);
            netBalances.put(creditor, creditorBalance - transferAmount);

            if (Math.abs(netBalances.get(debtor)) < 0.001) {
                debtorIndex++;
            }
            if (Math.abs(netBalances.get(creditor)) < 0.001) {
                creditorIndex++;
            }
        }

        return transactions;
    }

    public int getMinimumSettlements() {
        Map<User, Double> netBalances = new HashMap<>();

        for (Map.Entry<UserPair, Double> entry : balances.entrySet()) {
            UserPair pair = entry.getKey();
            double amount = entry.getValue();

            User debtor = pair.getUser1();
            User creditor = pair.getUser2();

            netBalances.put(debtor, netBalances.getOrDefault(debtor, 0.0) - amount);
            netBalances.put(creditor, netBalances.getOrDefault(creditor, 0.0) + amount);
        }

        List<Double> balancesList = netBalances.values().stream()
                .filter(b -> Math.abs(b) > 0.001)
                .collect(Collectors.toList());

        int n = balancesList.size();
        int[] dp = new int[1 << n];
        Arrays.fill(dp, -1);
        dp[0] = 0;

        return n - dfs((1 << n) - 1, dp, balancesList);
    }

    private int dfs(int mask, int[] dp, List<Double> balancesList) {
        if (mask == 0) return 0;
        if (dp[mask] != -1) return dp[mask];

        int maxSubGroups = 0;
        int n = balancesList.size();

        for (int submask = 1; submask < (1 << n); submask++) {
            if ((submask & mask) == submask && Math.abs(sumOfMask(balancesList, submask)) < 0.001) {
                maxSubGroups = Math.max(maxSubGroups, 1 + dfs(mask ^ submask, dp, balancesList));
            }
        }

        dp[mask] = maxSubGroups;
        return maxSubGroups;
    }

    private double sumOfMask(List<Double> values, int mask) {
        double sum = 0;
        for (int i = 0; i < values.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                sum += values.get(i);
            }
        }
        return sum;
    }
}

// Main class to demonstrate the system
public class SplitwiseSystem {
    public static void main(String[] args) {
        // Create users
        User alice = new User("u1", "Alice", "alice@example.com");
        User bob = new User("u2", "Bob", "bob@example.com");
        User charlie = new User("u3", "Charlie", "charlie@example.com");

        // Create expense manager and balance sheet
        ExpenseManager expenseManager = new ExpenseManager();
        BalanceSheet balanceSheet = new BalanceSheet();

        // Register the balance sheet as an observer
        expenseManager.addObserver(balanceSheet);

        // Create participants list
        List<User> participants = Arrays.asList(alice, bob, charlie);

        // Create an equal split expense
        Split equalSplit = SplitFactory.createSplit("EQUAL");
        Map<String, Object> splitDetails = new HashMap<>();
        Map<User, Double> dinnerShares = equalSplit.calculateSplit(60.0, participants, splitDetails);

        Expense dinnerExpense = new Expense("e1", "Dinner", 60.0, alice, participants, dinnerShares);
        expenseManager.addExpense(dinnerExpense);

        // Create a percentage split expense
        Map<String, Object> percentageDetails = new HashMap<>();
        Map<User, Double> percentages = new HashMap<>();
        percentages.put(alice, 40.0);
        percentages.put(bob, 30.0);
        percentages.put(charlie, 30.0);
        percentageDetails.put("percentages", percentages);

        Split percentageSplit = SplitFactory.createSplit("PERCENTAGE");
        Map<User, Double> movieShares = percentageSplit.calculateSplit(45.0, participants, percentageDetails);

        Expense movieExpense = new Expense("e2", "Movie", 45.0, bob, participants, movieShares);
        expenseManager.addExpense(movieExpense);

        // Display balances
        System.out.println("=== BALANCES ===");
        System.out.println("Alice's total balance: $" + String.format("%.2f", balanceSheet.getTotalBalance(alice)));
        System.out.println("Bob's total balance: $" + String.format("%.2f", balanceSheet.getTotalBalance(bob)));
        System.out.println("Charlie's total balance: $" + String.format("%.2f", balanceSheet.getTotalBalance(charlie)));

        System.out.println("\n=== PAIRWISE BALANCES ===");
        System.out.println("Alice owes Bob: $" + String.format("%.2f", balanceSheet.getBalance(alice, bob)));
        System.out.println("Alice owes Charlie: $" + String.format("%.2f", balanceSheet.getBalance(alice, charlie)));
        System.out.println("Bob owes Charlie: $" + String.format("%.2f", balanceSheet.getBalance(bob, charlie)));

        // Display settlements
        System.out.println("\n=== SIMPLIFIED SETTLEMENTS ===");
        List<Transaction> settlements = balanceSheet.getSimplifiedSettlements();
        for (Transaction transaction : settlements) {
            System.out.println(transaction);
        }

        // Display minimum transactions needed
        System.out.println("\n=== MINIMUM TRANSACTIONS NEEDED ===");
        System.out.println("Minimum transactions: " + balanceSheet.getMinimumSettlements());
    }
}
