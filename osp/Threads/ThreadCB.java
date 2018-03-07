package osp.Threads;
import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject osp.Threads
*/
public class ThreadCB extends IflThreadCB 
{


    //private static Vector<Sub_threads> ready_queue;
    private static ConcurrentHashMap<TaskCB,Vector> ready_queue;

    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject osp.Threads
    */
    public ThreadCB()
    {
        // your code goes here
        super();

    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject osp.Threads
    */
    public static void init()
    {
        // your code goes here
        //ready_queue = new Vector<Sub_threads>;
        ready_queue = new ConcurrentHashMap<TaskCB,Vector>();

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task,
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject osp.Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        // your code goes here
        if (task == null){
            return null;
        }

        if (task.getThreadCount() > MaxThreadsPerTask){
            return null;
        }

        Sub_threads thread = new Sub_threads();

        thread.setStatus(ThreadReady);

        thread.setTask(task);

        thread.setPriority(1.5*thread.get_total_wait_time() - thread.getTimeOnCPU() - 0.3*thread.getTask().getTimeOnCPU());


        if (task.addThread(thread) == FAILURE){
            return null;
        }

        if (ready_queue.containsKey(task) == false){
            Vector sub_queue = new Vector<Sub_threads>();
            sub_queue.addElement(thread);
            ready_queue.put(task, sub_queue);
        }else {
            Vector sub_queue = ready_queue.get(task);
            if (sub_queue != null){
                sub_queue.addElement(thread)
            }
        }
        //ready_queue.add(thread);

        dispatch();

        return thread;


    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject osp.Threads
    */
    public void do_kill()
    {
        // your code goes here
        if (getStatus() == ThreadReady){

            ready_queue.remove(this);

        }else if(getStatus() == ThreadRunning){

            if (MMU.getPTBR().getTask().getCurrentThread().getID() == this.getID()){
                MMU.setPTBR(null);
                getTask().setCurrentThread(null);
            }

        }

        setStatus(ThreadKill);

        //cancelling I/O

        for (ing i = 0; i < Device.getTableSize(); i++){
            Device.get(i).cancelPendingIO(this);
        }

        ResourceCB.giveupResources(this);

        dispatch();

        if (getTask().getThreadCount() == 0){
            getTask().kill();
        }

    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject osp.Threads
    */
    public void do_suspend(Event event)
    {
        // your code goes here
        if (this.getStatus() == ThreadRunning){


            //context switch
            if (MMU.getPTBR().getTask().getCurrentThread().getID() == this.getID()){
                MMU.setPTBR(null);
                getTask().setCurrentThread(null);
            }

            this.setStatus(ThreadWaiting);
            this.getTask().setCurrentThread(null);

        }else if(this.getStatus() >= ThreadKill && this.getStatus() != ThreadReady){

            this.setStatus(this.getStatus()+1);
        }

        /*if (ready_queue.contains(this) == false){
            ready_queue.add(this);
        }*/


        event.addThread(this);

        dispatch();


    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject osp.Threads
    */
    public void do_resume()
    {
        // your code goes here
        if (this.getStatus() > ThreadWaiting){
            this.setStatus(this.getStatus() - 1);
        }else if (this.getStatus() == ThreadWaiting){
            this.setStatus(ThreadReady);
            ready_queue.add()
        }




    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject osp.Threads
    */
    public static int do_dispatch()
    {
        // your code goes here

    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject osp.Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject osp.Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */





}

/*
      Feel free to add local classes to improve the readability of your code
*/
class Sub_threads extends ThreadCB{

    //private int total_cpu_time;
    private int total_wait_time;

    Sub_threads(){
        super();
        //total_cpu_time = 0;
        total_wait_time = 0;
    }

    /*int get_total_cpu_time(){
        return total_cpu_time;
    }*/

    int get_total_wait_time(){
        return total_wait_time;
    }

    /*int set_total_cpu_time(int t){
        total_cpu_time = t;
        setTotal_task_CPUTime(getTotal_task_CPUTime()+total_cpu_time);
    }*/

    int set_total_wait_time(int t){
        total_wait_time = t;
        this.setPriority(1.5*this.get_total_wait_time() - this.getTimeOnCPU() - 0.3*this.getTask().getTimeOnCPU());
    }
}

