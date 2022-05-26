/*
    Ewan Fulmer
    SE 4348.502

    Project 2 Restaurant.java
 */
import java.util.concurrent.*;
import java.util.*;

// Shared variable to be used for coordination between the processes
class Shared
{
    // Counter for number of people in the restaurant that are not in a line or seated at the table
    static int lobbyCount  = 0;
    // Map that stores data of the length of the line for each table A, B, and C
    static Map<String, Integer> lineCounts = new HashMap<>();
    //Map that stores data of the number seated for each table A, B, and C
    static Map<String, Integer> tableCounts = new HashMap<>();
    // Map that stores the id of the calling customer id for each table A, B, and C
    static Map<String, String> idCall = new HashMap<>();
    // Map that stores the id of the ordering customer for each table A, B, and C
    static Map<String, String> idOrder = new HashMap<>();
    // Array of strings to store the name of each of the three tables
    static String [] tableList = {"A", "B", "C"};

    // Semaphore for customers for each table
    static Semaphore semCustomerA = new Semaphore(1, true);
    static Semaphore semCustomerB = new Semaphore(1, true);
    static Semaphore semCustomerC = new Semaphore(1, true);
    // Semaphore for waiters for each table
    static Semaphore semWaiterA = new Semaphore(0, true);
    static Semaphore semWaiterB = new Semaphore(0, true);
    static Semaphore semWaiterC = new Semaphore(0, true);
    // A mutual exclusion semaphore used so only one customer can be helped by a waiter at a time
    static Semaphore semMutexA = new Semaphore(1, true);
    static Semaphore semMutexB = new Semaphore(1, true);
    static Semaphore semMutexC = new Semaphore(1, true);
    // Semaphore for a table. Allows up to four customer threads to be concurrent on a table.
    static Semaphore semTableA = new Semaphore(4, true);
    static Semaphore semTableB = new Semaphore(4, true);
    static Semaphore semTableC = new Semaphore(4, true);
    // Semaphore for the kitchen. Only one waiter may access the kitchen at a time
    static Semaphore semKitchen = new Semaphore (1, true);
    // Semaphore for the register for the customers to pay their bill. Only one customer allowed at a time.
    static Semaphore semRegister = new Semaphore (1, true);
}

// Thread Class for the Customer
class CustomerThread extends Thread
{
    // Strings for the choices of table. First choice is preferred but back up choice of second choice is used
    // when the line for the first choice is long and not long for the second choice.
    String firstChoice, secondChoice;
    // Semaphores matching the ones in the shared class
    Semaphore semCustomer, semWaiter, semMutex, semTable, semRegister;
    // id is the identifier for the customer thread. table identifies which table and which waiter they are assigned
    // to.
    String id, table;
    // int for which door the customer walks through door 1 or door 2
    int door;
    // Random object for assigning table
    Random rand = new Random();

    // Constructor for customer thread
    public CustomerThread(String id, int door)
    {
        super(id);
        this.id = id;
        this.door = door;
        this.semRegister = Shared.semRegister;
        // Randomly get first choice
        this.firstChoice = Shared.tableList[rand.nextInt(3)];
        System.out.println("Customer " + id + " walks through door " + door);
        // Increment lobby count as customer is neither in a line or seated at a table
        Shared.lobbyCount++;
        // If random integer is 0 then the customer does not have a second choice. Assign first choice as table.
        if(rand.nextInt(2) == 0)
        {
            this.secondChoice = "";
            this.table = firstChoice;
            System.out.println("Customer " + id + "'s first choice is " + firstChoice + ". No second choice.");
            System.out.println("Customer " + id + " chooses their first choice " + table);
            // Assign appropriate semaphores from the Shared class based on which table they choose
            if (this.table.equals("A"))
            {
                this.semCustomer = Shared.semCustomerA;
                this.semWaiter = Shared.semWaiterA;
                this.semMutex = Shared.semMutexA;
                this.semTable = Shared.semTableA;
            }
            else if(this.table.equals("B"))
            {
                this.semCustomer = Shared.semCustomerB;
                this.semWaiter = Shared.semWaiterB;
                this.semMutex = Shared.semMutexB;
                this.semTable = Shared.semTableB;
            }
            else
            {
                this.semCustomer = Shared.semCustomerC;
                this.semWaiter = Shared.semWaiterC;
                this.semMutex = Shared.semMutexC;
                this.semTable = Shared.semTableC;
            }

        }
        // If the random integer is not 0. Then the customer has a second choice
        else
        {
            // Randomly get second choice
            this.secondChoice = Shared.tableList[rand.nextInt(3)];
            System.out.println("Customer " + id + "'s first choice is " + firstChoice + ". Second choice is " +
                    secondChoice);
            // iF the line for the first choice is longer than or equal to 7
            if(Shared.lineCounts.get(firstChoice) >= 7)
            {
                // If the second choice line is not long then the customer chooses the second choice
                if(Shared.lineCounts.get(secondChoice) < 7)
                {
                    this.table = secondChoice;
                    System.out.println("Customer " + id + " chooses their second choice " + table + " " +
                            "due to long line at" +
                            " first choice");
                }
                // If the second choice's line is also long then the customer sticks with the first choice
                else
                {
                    this.table = firstChoice;
                    System.out.println("Customer " + id + " chooses their first choice " + table);
                }
            }
            // If the first choice line is not long then the customer just sticks with first choice
            else
            {
                this.table = firstChoice;
                System.out.println("Customer " + id + " chooses their first choice " + table);
            }
            // Assign appropriate semaphores from the Shared class based on table choice
            if (this.table.equals("A"))
            {
                this.semCustomer = Shared.semCustomerA;
                this.semWaiter = Shared.semWaiterA;
                this.semMutex = Shared.semMutexA;
                this.semTable = Shared.semTableA;
            }
            else if(this.table.equals("B"))
            {
                this.semCustomer = Shared.semCustomerB;
                this.semWaiter = Shared.semWaiterB;
                this.semMutex = Shared.semMutexB;
                this.semTable = Shared.semTableB;
            }
            else
            {
                this.semCustomer = Shared.semCustomerC;
                this.semWaiter = Shared.semWaiterC;
                this.semMutex = Shared.semMutexC;
                this.semTable = Shared.semTableC;
            }
        }
    }

    // run function for customer thread
    public void run()
    {
        try
        {
            // Increment line value in the map for the table choice and decrement lobby count
            Shared.lineCounts.put(table, Shared.lineCounts.get(table) + 1);
            Shared.lobbyCount--;
            System.out.println("Customer " + id + " gets into the line " + table);
            // wait on table sempahore before being seated
            semTable.acquire();
            System.out.println("Customer " + id + " is seated at table " + table);
            // decrement line count and increment table count
            Shared.lineCounts.put(table, Shared.lineCounts.get(table) - 1);
            Shared.tableCounts.put(table, Shared.tableCounts.get(table) + 1);
            // Mutex ensures that waiter only helps one customer at a time
            semMutex.acquire();
            // Call for waiter. Put id of calling customer in the idCall map
            Shared.idCall.put(table, id);
            semCustomer.acquire();
            System.out.println("Customer " + id + " calls for the waiter " + table);
            semWaiter.release();
            semCustomer.acquire();
            // Give the order to the waiter. Put id of ordering customer in the map idOrder
            Shared.idOrder.put(table, id);
            System.out.println("Customer " + id + " gives order to the waiter " + table);
            semWaiter.release();
            semCustomer.acquire();
            // After reaceiving food Customer eats for a random time between 200 and 1000 milliseconds
            System.out.println("Customer " + id + " eats food");
            ThreadLocalRandom.current().nextInt(200, 1000 + 1);
            // wait on register semaphore. THen pay and leave. Decrement table count.
            semRegister.acquire();
            System.out.println("Customer " + id + " pays the bill and leaves");
            semRegister.release();
            Shared.tableCounts.put(table, Shared.tableCounts.get(table) - 1);
            semWaiter.release();
            semCustomer.acquire();
            semCustomer.release();
            semMutex.release();
            semTable.release();
        }catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}

// WaiterThread class
class WaiterThread extends Thread
{
    // Semaphores matching the ones in the Shared class
    Semaphore semCustomer, semWaiter, semKitchen;
    // String for which table the waiter is assigned to
    String table;

    // Constructor for Waiter Thread
    public WaiterThread(String table)
    {
        super(table);
        this.table = table;
        this.semKitchen = Shared.semKitchen;

        // Assign semaphores based on which table the waiter is assigned to
        if(this.table.equals("A"))
        {
            this.semCustomer = Shared.semCustomerA;
            this.semWaiter = Shared.semWaiterA;
        }
        else if(this.table.equals("B"))
        {
            this.semCustomer = Shared.semCustomerB;
            this.semWaiter = Shared.semWaiterB;
        }
        else
        {
            this.semCustomer = Shared.semCustomerC;
            this.semWaiter = Shared.semWaiterC;
        }
        System.out.println("A waiter is assigned to table " + table);
    }

    public void run()
    {
        try
        {
            while(true)
            {
                // After getting signal from customer inform them waiter informs the customer they are ready to take
                // the order
                semWaiter.acquire();
                System.out.println("Waiter " + table + " informs customer " + Shared.idCall.get(table) + " " +
                        "that they are ready");
                semCustomer.release();
                semWaiter.acquire();
                // After customer gives order, take it from the share map idOrder
                System.out.println("Waiter " + table + " takes customer " + Shared.idOrder.get(table) + "'s order");
                // Wait for kitchen. After it is the waiter's turn, deliver the order, wait for it, and take the order
                semKitchen.acquire();
                System.out.println("Waiter " + table + " delivers order " + Shared.idOrder.get(table) + " to kitchen");
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500 + 1));
                System.out.println("Waiter " + table + " waits for order " + Shared.idOrder.get(table)  + " " +
                        "from the kitchen");
                ThreadLocalRandom.current().nextInt(300, 1000 + 1);
                System.out.println("Waiter " + table + " goes into the kitchen to get order " +
                        Shared.idOrder.get(table));
                ThreadLocalRandom.current().nextInt(100, 500 + 1);
                System.out.println("Waiter " + table + " delivers order to customer " + Shared.idOrder.get(table));
                semKitchen.release();
                semCustomer.release();
                // Wait for the next customer. If there are none. Terminate the thread
                semWaiter.acquire();
                System.out.println("Waiter " + table + " waits for the next customer");
                if(Shared.tableCounts.get(table) == 0 && Shared.lineCounts.get(table) == 0 && Shared.lobbyCount == 0)
                {
                    System.out.println("No more customers. " + "Waiter " + table + " cleans up and clocks out");
                    this.interrupt();
                }
                semCustomer.release();
            }
        }catch(InterruptedException e)
        {

        }
    }
}

public class Restaurant
{
    public static void main(String[] args) throws InterruptedException
    {
        // Initialize map values
        Shared.lineCounts.put("A", 0);
        Shared.lineCounts.put("B", 0);
        Shared.lineCounts.put("C", 0);
        Shared.tableCounts.put("A", 0);
        Shared.tableCounts.put("B", 0);
        Shared.tableCounts.put("C", 0);

        // Start the 3 waiter threads with assigned tables
        WaiterThread waiterA = new WaiterThread("A");
        WaiterThread waiterB = new WaiterThread("B");
        WaiterThread waiterC = new WaiterThread("C");

        // Start waiter threads
        waiterA.start();
        waiterB.start();
        waiterC.start();

        // Create an array of 40 customers and start them 2 at a time as there are 2 doors to the restaurant
        CustomerThread [] customerThreads = new CustomerThread[40];
        for(int i = 0; i < 40; i = i + 2)
        {
            customerThreads[i] = new CustomerThread(String.valueOf(i),1);
            customerThreads[i+1] = new CustomerThread(String.valueOf(i+1),2);
            customerThreads[i].start();
            customerThreads[i+1].start();
        }

        for(int i = 0; i < 40; i++)
        {
            customerThreads[i].join();
        }
        waiterA.join();
        waiterB.join();
        waiterC.join();
    }
}