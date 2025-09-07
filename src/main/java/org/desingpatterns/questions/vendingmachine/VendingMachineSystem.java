package org.desingpatterns.questions.vendingmachine;

import java.util.ArrayList;
import java.util.List;

// Enum representing different types of items in the vending machine
enum ItemType {
    COKE,
    PEPSI,
    JUICE,
    SODA
}

// Class representing an item in the vending machine
class Item {
    private ItemType type;
    private int price;

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}

// Enum representing different types of coins
enum Coin {
    ONE_RUPEE(1),
    TWO_RUPEES(2),
    FIVE_RUPEES(5),
    TEN_RUPEES(10);

    public final int value;

    Coin(int value) {
        this.value = value;
    }
}

// Class representing a slot in the vending machine
class ItemShelf {
    private int code;
    private List<Item> items;
    private boolean isSoldOut;

    public ItemShelf(int code) {
        this.code = code;
        this.items = new ArrayList<>();
        this.isSoldOut = false;
    }

    public int getCode() {
        return code;
    }

    public List<Item> getItems() {
        return items;
    }

    public boolean checkIsSoldOut() {
        return isSoldOut;
    }

    public void setIsSoldOut(boolean isSoldOut) {
        this.isSoldOut = isSoldOut;
    }

    public void addItem(Item item) {
        items.add(item);
        if (isSoldOut) setIsSoldOut(false);
    }

    public void removeItem(Item item) {
        items.remove(item);
        if (items.isEmpty()) setIsSoldOut(true);
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }
}

// Class representing the inventory of the vending machine
class Inventory {
    private ItemShelf[] inventory;

    public Inventory(int itemCount) {
        inventory = new ItemShelf[itemCount];
        initialEmptyInventory();
    }

    public ItemShelf[] getInventory() {
        return inventory;
    }

    public void initialEmptyInventory() {
        int startCode = 101;
        for (int i = 0; i < inventory.length; i++) {
            ItemShelf space = new ItemShelf(startCode);
            inventory[i] = space;
            startCode++;
        }
    }

    public void addItem(Item item, int codeNumber) throws Exception {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                itemShelf.addItem(item);
                return;
            }
        }
        throw new Exception("Invalid Code");
    }

    public Item getItem(int codeNumber) throws Exception {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                if (itemShelf.checkIsSoldOut()) {
                    throw new Exception("Item already sold out");
                } else {
                    return itemShelf.getItems().get(0);
                }
            }
        }
        throw new Exception("Invalid Code");
    }

    public void updateSoldOutItem(int codeNumber) {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                if (itemShelf.getItems().isEmpty())
                    itemShelf.setIsSoldOut(true);
            }
        }
    }

    public void removeItem(int codeNumber) throws Exception {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                itemShelf.removeItem(itemShelf.getItems().get(0));
                return;
            }
        }
        throw new Exception("Invalid Code");
    }

    public boolean hasItems() {
        for (ItemShelf itemShelf : inventory) {
            if (!itemShelf.checkIsSoldOut()) return true;
        }
        return false;
    }
}

// Factory class to initialize inventory with default products
class InventoryFactory {
    public static void initializeDefaultInventory(VendingMachineContext vendingMachine) {
        for (int i = 0; i < 10; i++) {
            Item newItem = new Item();
            int codeNumber = 101 + i;

            if (i >= 0 && i < 3) {
                newItem.setType(ItemType.COKE);
                newItem.setPrice(12);
            } else if (i >= 3 && i < 5) {
                newItem.setType(ItemType.PEPSI);
                newItem.setPrice(9);
            } else if (i >= 5 && i < 7) {
                newItem.setType(ItemType.JUICE);
                newItem.setPrice(13);
            } else if (i >= 7 && i < 10) {
                newItem.setType(ItemType.SODA);
                newItem.setPrice(7);
            }

            for (int j = 0; j < 5; j++) {
                vendingMachine.updateInventory(newItem, codeNumber);
            }
        }
    }
}

// State Pattern: Interface for vending machine states
interface VendingMachineState {
    String getStateName();
    VendingMachineState next(VendingMachineContext context);
}

// Concrete state: Idle state
class IdleState implements VendingMachineState {
    @Override
    public String getStateName() {
        return "IdleState";
    }

    @Override
    public VendingMachineState next(VendingMachineContext context) {
        if (!context.getInventory().hasItems()) {
            return new OutOfStockState();
        }
        if (!context.getCoinList().isEmpty()) {
            return new HasMoneyState();
        }
        return this;
    }
}

// Concrete state: HasMoney state
class HasMoneyState implements VendingMachineState {
    @Override
    public String getStateName() {
        return "HasMoneyState";
    }

    @Override
    public VendingMachineState next(VendingMachineContext context) {
        if (!context.getInventory().hasItems()) {
            return new OutOfStockState();
        }
        if (context.getCoinList().isEmpty()) {
            return new IdleState();
        }
        return new SelectionState();
    }
}

// Concrete state: Selection state
class SelectionState implements VendingMachineState {
    @Override
    public String getStateName() {
        return "SelectionState";
    }

    @Override
    public VendingMachineState next(VendingMachineContext context) {
        if (!context.getInventory().hasItems()) {
            return new OutOfStockState();
        }
        if (context.getCoinList().isEmpty()) {
            return new IdleState();
        }
        if (context.getSelectedItemCode() > 0) {
            return new DispenseState();
        }
        return this;
    }
}

// Concrete state: Dispense state
class DispenseState implements VendingMachineState {
    @Override
    public String getStateName() {
        return "DispenseState";
    }

    @Override
    public VendingMachineState next(VendingMachineContext context) {
        return new IdleState();
    }
}

// Concrete state: OutOfStock state
class OutOfStockState implements VendingMachineState {
    @Override
    public String getStateName() {
        return "OutOfStockState";
    }

    @Override
    public VendingMachineState next(VendingMachineContext context) {
        if (context.getInventory().hasItems()) {
            return new IdleState();
        }
        return this;
    }
}

// Context class for the state pattern
class VendingMachineContext {
    private VendingMachineState currentState;
    private Inventory inventory;
    private List<Coin> coinList;
    private int selectedItemCode;

    public VendingMachineContext() {
        inventory = new Inventory(10);
        coinList = new ArrayList<>();
        currentState = new IdleState();
        System.out.println("Initialized: " + currentState.getStateName());
    }

    public VendingMachineState getCurrentState() {
        return currentState;
    }

    public void advanceState() {
        VendingMachineState nextState = currentState.next(this);
        currentState = nextState;
        System.out.println("Current state: " + currentState.getStateName());
    }

    public void clickOnInsertCoinButton(Coin coin) {
        if (currentState instanceof IdleState || currentState instanceof HasMoneyState) {
            System.out.println("Inserted " + coin.name() + " worth " + coin.value);
            coinList.add(coin);
            advanceState();
        } else {
            System.out.println("Cannot insert coin in " + currentState.getStateName());
        }
    }

    public void clickOnStartProductSelectionButton(int codeNumber) {
        if (currentState instanceof HasMoneyState) {
            advanceState();
            selectProduct(codeNumber);
        } else {
            System.out.println("Product selection button can only be clicked in HasMoney state");
        }
    }

    public void selectProduct(int codeNumber) {
        if (currentState instanceof SelectionState) {
            try {
                Item item = inventory.getItem(codeNumber);
                int balance = getBalance();
                if (balance < item.getPrice()) {
                    System.out.println("Insufficient amount. Product price: " + item.getPrice() + ", paid: " + balance);
                    return;
                }
                setSelectedItemCode(codeNumber);
                advanceState();
                dispenseItem(codeNumber);

                if (balance > item.getPrice()) {
                    int change = balance - item.getPrice();
                    System.out.println("Returning change: " + change);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else {
            System.out.println("Products can only be selected in Selection state");
        }
    }

    public void dispenseItem(int codeNumber) {
        if (currentState instanceof DispenseState) {
            try {
                Item item = inventory.getItem(codeNumber);
                System.out.println("Dispensing: " + item.getType());
                inventory.removeItem(codeNumber);
                inventory.updateSoldOutItem(codeNumber);
                resetBalance();
                resetSelection();
                advanceState();
            } catch (Exception e) {
                System.out.println("Failed to Dispense the Product with code : " + codeNumber);
            }
        } else {
            System.out.println("System cannot dispense in : " + currentState);
        }
    }

    public void updateInventory(Item item, int codeNumber) {
        if (currentState instanceof IdleState) {
            try {
                inventory.addItem(item, codeNumber);
                System.out.println("Added " + item.getType() + " to slot " + codeNumber);
            } catch (Exception e) {
                System.out.println("Error updating inventory: " + e.getMessage());
            }
        } else {
            System.out.println("Inventory can only be updated in Idle state");
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public List<Coin> getCoinList() {
        return coinList;
    }

    public int getSelectedItemCode() {
        return selectedItemCode;
    }

    public void setSelectedItemCode(int codeNumber) {
        this.selectedItemCode = codeNumber;
    }

    public void resetSelection() {
        this.selectedItemCode = 0;
    }

    public int getBalance() {
        int balance = 0;
        for (Coin coin : coinList) {
            balance += coin.value;
        }
        return balance;
    }

    public void resetBalance() {
        coinList.clear();
    }
}

// Utility class to display inventory
class InventoryDisplay {
    public static void displayInventory(VendingMachineContext vendingMachine) {
        ItemShelf[] slots = vendingMachine.getInventory().getInventory();
        for (ItemShelf slot : slots) {
            List<Item> items = slot.getItems();
            if (!items.isEmpty()) {
                System.out.println("CodeNumber: " + slot.getCode() + " Items: ");
                for (Item item : items) {
                    System.out.println("    - Item: " + item.getType().name() + ", Price: " + item.getPrice());
                }
                System.out.println("SoldOut: " + slot.checkIsSoldOut());
            } else {
                System.out.println("CodeNumber: " + slot.getCode() + " Items: EMPTY" + " SoldOut: " + slot.checkIsSoldOut());
            }
        }
    }
}

// Main class to demonstrate the vending machine
public class VendingMachineSystem {
    public static void main(String args[]) {
        VendingMachineContext vendingMachine = new VendingMachineContext();
        try {
            System.out.println("|");
            System.out.println("Filling up the inventory");
            System.out.println("|");

            // Use the factory to initialize inventory
            InventoryFactory.initializeDefaultInventory(vendingMachine);

            InventoryDisplay.displayInventory(vendingMachine);
            System.out.println("|");
            System.out.println("Inserting coins");
            System.out.println("|");
            vendingMachine.clickOnInsertCoinButton(Coin.TEN_RUPEES);
            vendingMachine.clickOnInsertCoinButton(Coin.FIVE_RUPEES);
            System.out.println("|");
            System.out.println("Clicking on ProductSelectionButton");
            System.out.println("|");
            vendingMachine.clickOnStartProductSelectionButton(102);
            InventoryDisplay.displayInventory(vendingMachine);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            InventoryDisplay.displayInventory(vendingMachine);
        }
    }
}