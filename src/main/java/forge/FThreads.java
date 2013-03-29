package forge;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import forge.control.input.InputSynchronized;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FThreads {
    static { 
        System.out.printf("(FThreads static ctor): Running on a machine with %d cpu core(s)%n", Runtime.getRuntime().availableProcessors() );
    }
    
    private FThreads() { } // no instances supposed
    
    private final static ExecutorService cachedPool = Executors.newCachedThreadPool();
    private static ExecutorService getCachedPool() { return cachedPool; }
    private final static ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2);
    private static ScheduledExecutorService getScheduledPool() { return scheduledPool; }

    // This pool is designed to parallel CPU or IO intensive tasks like parse cards or download images, assuming a load factor of 0.5
    public final static ExecutorService getComputingPool(float loadFactor) {
        return Executors.newFixedThreadPool((int)(Runtime.getRuntime().availableProcessors() / (1-loadFactor)));
    }
    
    public static boolean isMultiCoreSystem() {
        return Runtime.getRuntime().availableProcessors() > 1;
    }

    /** Checks if calling method uses event dispatch thread.
     * Exception thrown if method is on "wrong" thread.
     * A boolean is passed to indicate if the method must be EDT or not.
     * 
     * @param methodName &emsp; String, part of the custom exception message.
     * @param mustBeEDT &emsp; boolean: true = exception if not EDT, false = exception if EDT
     */
    public static void checkEDT(final String methodName, final boolean mustBeEDT) {
        boolean isEDT = SwingUtilities.isEventDispatchThread();
        if ( isEDT != mustBeEDT ) { 
            String modalOperator = mustBeEDT ? " must be" : " may not be";
            throw new IllegalStateException( methodName + modalOperator + " accessed from the event dispatch thread.");
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param runnable
     */
    public static void invokeInEDT(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * Invoke the given Runnable in an Event Dispatch Thread and wait for it to
     * finish; but <B>try to use SwingUtilities.invokeLater instead whenever
     * feasible.</B>
     * 
     * Exceptions generated by SwingUtilities.invokeAndWait (if used), are
     * rethrown as RuntimeExceptions.
     * 
     * @param proc
     *            the Runnable to run
     * @see javax.swing.SwingUtilities#invokeLater(Runnable)
     */
    public static void invokeInEDTAndWait(final Runnable proc) {
        if (SwingUtilities.isEventDispatchThread()) {
            // Just run in the current thread.
            proc.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(proc);
            } catch (final InterruptedException exn) {
                throw new RuntimeException(exn);
            } catch (final InvocationTargetException exn) {
                throw new RuntimeException(exn);
            }
        }
    }
    
    
    public static void invokeInNewThread(Runnable toRun) {
        getCachedPool().execute(toRun);
    }
    
    public static void invokeInNewThread(final Runnable proc, boolean lockUI) {
        Runnable toRun = proc;
        if( lockUI ) {
            // checkEDT("FThreads.invokeInNewthread", true)
            Singletons.getModel().getMatch().getInput().lock();
            toRun = new Runnable() {
                @Override
                public void run() {
                    proc.run();
                    Singletons.getModel().getMatch().getInput().unlock();
                }
            };
        }
        invokeInNewThread(toRun);
    }
    
    public static void setInputAndWait(InputSynchronized input) {
        Singletons.getModel().getMatch().getInput().setInput(input);
        input.awaitLatchRelease();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public static boolean isEDT() {
        return SwingUtilities.isEventDispatchThread();
    }


    public static void delay(int milliseconds, Runnable inputUpdater) {
        getScheduledPool().schedule(inputUpdater, milliseconds, TimeUnit.MILLISECONDS);
    }
   
}
