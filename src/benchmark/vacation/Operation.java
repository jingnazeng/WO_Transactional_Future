package benchmark.vacation;

public abstract class Operation {

    public static int numberOfTransactionalFuture;
    public static boolean parallelizeUpdateTables;
    public static boolean readOnly;
    public static boolean unsafe;
	public static boolean transactionalFutureEnabled;
    public Integer futureAborts=0;
    public Integer futuresEarlyAborts =0;

    public abstract void doOperation() throws Throwable;

}
