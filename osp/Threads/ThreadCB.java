/**
 * @Author Weifeng Lin
 * @StudentID 110161112
 */
package osp.Threads;
import java.util.Comparator;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject osp.Threads
*/
public class ThreadCB extends IflThreadCB 
{


    //private static Vector<Sub_threads> ready_queue;
    private static ConcurrentHashMap<TaskCB,Vector<ThreadCB>> ready_queue;
    //private static Vector<Vector<Sub_threads>> ready_queue;
    //private static PriorityBlockingQueue<TaskCB> task_queue;
    //private static Vector<TaskCB> task_queue;
    private static ConcurrentLinkedQueue<TaskCB> task_queue;

    private long total_wait_time;
    //private long previous_waiting_time;
    //private long previous_cpu_time;
    private long time_added_to_ready_queue;
    private long time_removed_from_ready_queue;

//    public static Comparator<Vector<Sub_threads>> priorityComparator = new Comparator<Vector<Sub_threads>>() {
//        @Override
//        public int compare(Vector<Sub_threads> o1, Vector<Sub_threads> o2) {
//            int diff = 0;
//            try{
//                diff = o2.get(0).getTask().getPriority() - o1.get(0).getTask().getPriority();
//            }catch (Exception e){}
//            return diff;
//        }
//    };

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
        this.time_added_to_ready_queue = 0;
        this.time_removed_from_ready_queue = 0;
        this.total_wait_time = 0;

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
        ready_queue = new ConcurrentHashMap<>();
        //ready_queue = new PriorityBlockingQueue<>();
        //ready_queue = new Vector<>();
        //task_queue = new PriorityBlockingQueue<>();
        //task_queue = new Vector<>();
        task_queue = new ConcurrentLinkedQueue<>();

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
            //dispatch();
            return null;
        }

        if (task.getThreadCount() > MaxThreadsPerTask){
            dispatch();
            return null;
        }

        if (!task_queue.contains(task)){
            //task_queue.addElement(task);
            task_queue.add(task);
        }

        //Sub_threads thread = new Sub_threads();
        ThreadCB thread = new ThreadCB();

        thread.setStatus(ThreadReady);

        thread.setTask(task);



        long taskCPUTime = 0;

        if (!ready_queue.isEmpty() && ready_queue.containsKey(task)){
            //Vector<Sub_threads> v = ready_queue.get(task);
            Vector<ThreadCB> v = ready_queue.get(task);
            for(int i=0; i<v.size(); i++){
                taskCPUTime += v.get(i).getTimeOnCPU();
            }
        }


        if (task.addThread(thread) == FAILURE){
            //dispatch();
            return null;
        }




        //Vector<Sub_threads> sub_queue = ready_queue.get(task);
        Vector<ThreadCB> sub_queue = ready_queue.get(task);

        if (/*!ready_queue.containsKey(task)*/ sub_queue == null){

            sub_queue = new Vector<>();
            thread.setTime_added_to_ready_queue(HClock.get());
            thread.setTime_removed_from_ready_queue(HClock.get());
            thread.setPriority((int) (1.5*thread.getTotal_wait_time() - thread.getTimeOnCPU() - 0.3*taskCPUTime));

            sub_queue.addElement(thread);


            ready_queue.put(task, sub_queue);
        }else {
            //Vector<Sub_threads> sub_queue = ready_queue.get(task);
            //if (sub_queue != null){

                thread.setTime_added_to_ready_queue(HClock.get());
                thread.setTime_removed_from_ready_queue(HClock.get());
                thread.setPriority((int) (1.5*thread.getTotal_wait_time() - thread.getTimeOnCPU() - 0.3*taskCPUTime));

                //sub_queue.addElement(thread);
                ready_queue.get(task).addElement(thread);
                //ready_queue.put(task, sub_queue);
                //ready_queue.replace(task, sub_queue);

        }


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
        if (this.getStatus() == ThreadReady){

            //ready_queue.remove(this.getTask(), this);
            ready_queue.get(this.getTask()).remove(this);
            //this.setStatus(ThreadKill);

        }else if(this.getStatus() == ThreadRunning){

            if (MMU.getPTBR().getTask().getCurrentThread().getID() == this.getID()){
                MMU.setPTBR(null);
                getTask().setCurrentThread(null);
                //this.setStatus(ThreadKill);
            }

        }


        this.setStatus(ThreadKill);


        TaskCB task = this.getTask();
        task.removeThread(this);


        //cancelling I/O

        for (int i = 0; i < Device.getTableSize(); i++){
            Device.get(i).cancelPendingIO(this);
        }

        ResourceCB.giveupResources(this);

        if (getTask().getThreadCount() == 0){
            ready_queue.remove(getTask());
            task_queue.remove(getTask());
            getTask().kill();
        }

        dispatch();

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
        //Vector<Sub_threads> v ;
        Vector<ThreadCB> v;
        //Sub_threads thread = (Sub_threads) this;
        ThreadCB thread = this;

        if (thread.getStatus() > ThreadWaiting){

            thread.setStatus(this.getStatus() - 1);

        }else if (thread.getStatus() == ThreadWaiting ){
            thread.setStatus(ThreadReady);
             v = ready_queue.get(this.getTask());

            thread.setTime_added_to_ready_queue(HClock.get());
            thread.setTime_removed_from_ready_queue(HClock.get());

            long taskCPUTime = 0;

            for(int i=0; i<v.size(); i++){
                taskCPUTime += v.get(i).getTimeOnCPU();
            }
            thread.setPriority((int) (1.5*thread.getTotal_wait_time() - thread.getTimeOnCPU() - 0.3*taskCPUTime));

            //v.addElement((Sub_threads) this);
            //ready_queue.replace(thread.getTask(), v);
            ready_queue.get(thread.getTask()).addElement(thread);
        }

        dispatch();

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



        TaskCB currentTask = null;
        //Sub_threads thread = null;
        ThreadCB thread = null;
        //Vector<Sub_threads> v = null;
        Vector<ThreadCB> v=null;

        try {

            currentTask = MMU.getPTBR().getTask();
            //thread = (Sub_threads) currentTask.getCurrentThread();
            thread = currentTask.getCurrentThread();
            v = ready_queue.get(currentTask);

        }catch (Exception e){}








        if(thread != null)
        {
            thread.getTask().setCurrentThread(null);
            MMU.setPTBR(null);
            thread.setStatus(ThreadReady);
            if (!task_queue.contains(thread.getTask())){
                //task_queue.addElement(thread.getTask());
                task_queue.add(thread.getTask());
            }
            if (ready_queue.containsKey(thread.getTask())){

                thread.setTime_added_to_ready_queue(HClock.get());
                //thread.setTime_removed_from_ready_queue(HClock.get());

//                long taskCPUTime = 0;
//
//                for(int i=0; i<v.size(); i++){
//                    taskCPUTime += v.get(i).getTimeOnCPU();
//                }
//
//                thread.setPriority((int) (1.5*thread.getTotal_wait_time() - thread.getTimeOnCPU() - 0.3*taskCPUTime));

                ready_queue.get(thread.getTask()).addElement(thread);
                //v.addElement(thread);

                //ready_queue.replace(thread.getTask(),v);
            }
        }



        //if ((currentTask == null ||  (isBusyTask(v) && thread == null)  || (currentTask != null && v.isEmpty()) )){
        if ( (isBusyTask(v)  || (currentTask!=null && v.isEmpty()) )){
            //currentTask = task_queue.get(0);
            //currentTask = task_queue.poll();
            for(int i=0; i<task_queue.size(); i++){
                currentTask = task_queue.poll();
                v = ready_queue.get(currentTask);
                if (isBusyTask(v) || v.isEmpty()){
                    task_queue.add(currentTask);
                }else {
                    break;
                }
            }

            if (MMU.getPTBR()==null && currentTask != null){
                MMU.setPTBR(currentTask.getPageTable());
            }
            //thread = (Sub_threads) currentTask.getCurrentThread();
            //thread =  currentTask.getCurrentThread();
            v = ready_queue.get(currentTask);
            //task_queue.remove(currentTask);
            //task_queue.addElement(currentTask);

            if (!task_queue.contains(currentTask)){
                task_queue.add(currentTask);
            }

        }


        MyOut.print(task_queue, "task queue is"+task_queue);
        MyOut.print(v, "The queue for current task "+currentTask+" is " + v);

        if (v != null &&  !v.isEmpty()){


            //updating all priority
            long taskCPUTime = 0;

            for(int i=0; i<v.size(); i++){
                taskCPUTime += v.get(i).getTimeOnCPU();
            }

            for(int i=0; i<v.size(); i++){
//                Sub_threads t = v.get(i);
//                t.setTime_removed_from_ready_queue(HClock.get());
//                t.setPriority((int) (1.5*t.getTotal_wait_time() - t.getTimeOnCPU() - 0.3*taskCPUTime));
                ready_queue.get(currentTask).get(i).setTime_removed_from_ready_queue(HClock.get());
                int priority = (int) (1.5*ready_queue.get(currentTask).get(i).getTotal_wait_time()-ready_queue.get(currentTask).get(i).getTimeOnCPU()-0.3*taskCPUTime);
                ready_queue.get(currentTask).get(i).setPriority(priority);
            }

            //Sub_threads new_thread = v.get(0);
            //Sub_threads new_thread = ready_queue.get(currentTask).get(0);
            ThreadCB new_thread = ready_queue.get(currentTask).get(0);

            int i;
            for(i = 0; i<v.size(); i++ ){
                if (new_thread.getPriority() <= ready_queue.get(currentTask).get(i).getPriority() && ready_queue.get(currentTask).get(i).getStatus() == ThreadReady){
                    new_thread = ready_queue.get(currentTask).get(i);
                }
            }

            if (new_thread.getStatus() != ThreadReady){

//                new_thread.setTime_added_to_ready_queue(HClock.get());
//                new_thread.setTime_removed_from_ready_queue(HClock.get());
//                new_thread.setPriority((int) (1.5*new_thread.getTotal_wait_time() - new_thread.getTimeOnCPU() - 0.3*taskCPUTime));
//                v.remove(0);
//                v.addElement(new_thread);
//                ready_queue.replace(new_thread.getTask(), v);
//                new_thread.getTask().setCurrentThread(null);
                MMU.setPTBR(null);
                return FAILURE;
            }
            MyOut.print(new_thread, "The thread "+new_thread +" should be running.");

            if (!task_queue.contains(new_thread.getTask())){
                //task_queue.addElement(new_thread.getTask());
                task_queue.add(new_thread.getTask());
            }

            MMU.setPTBR(new_thread.getTask().getPageTable());
            new_thread.getTask().setCurrentThread(new_thread);

            new_thread.setStatus(ThreadRunning);
            //ready_queue.remove(new_thread.getTask(), new_thread);
            ready_queue.get(new_thread.getTask()).remove(new_thread);
            new_thread.setTime_removed_from_ready_queue(HClock.get());


            //new_thread.setPriority((int) (1.5*new_thread.getTotal_wait_time() - new_thread.getTimeOnCPU() - 0.3*taskCPUTime));

            HTimer.set(100);

            return SUCCESS;
        }

        MMU.setPTBR(null);
        return FAILURE;


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

    /*void setPriority(Sub_threads thread){
        thread.setTime_added_to_ready_queue(HClock.get());
        thread.setTime_removed_from_ready_queue(HClock.get());

        long taskCPUTime = 0;

        Vector<Sub_threads> v = ready_queue.get(thread.getTask());

        for(int i=0; i<v.size(); i++){
            taskCPUTime += v.get(i).getTimeOnCPU();
        }
        thread.setPriority((int) (1.5*thread.getTotal_wait_time() - thread.getTimeOnCPU() - 0.3*taskCPUTime));
    }*/


    private static boolean isBusyTask(Vector<ThreadCB> queue){
        if (queue == null){
            return true;
        }

        for(int i=0; i<queue.size(); i++){
            if (queue.get(i).getStatus() == ThreadReady){
                return false;
            }
        }

        return true;
    }


    void setTime_added_to_ready_queue(long time_added_to_ready_queue){
        this.time_added_to_ready_queue = time_added_to_ready_queue;
    }

    void setTime_removed_from_ready_queue(long time_removed_from_ready_queue){
        this.time_removed_from_ready_queue = time_removed_from_ready_queue;
    }
    long getTotal_wait_time(){
        total_wait_time = total_wait_time + time_removed_from_ready_queue - time_added_to_ready_queue;
        return total_wait_time;
    }

}

/*
      Feel free to add local classes to improve the readability of your code
*/
/*class Sub_threads extends ThreadCB{

    //private long total_cpu_time;
    private long total_wait_time;
    //private long previous_waiting_time;
    //private long previous_cpu_time;
    private long time_added_to_ready_queue;
    private long time_removed_from_ready_queue;


    Sub_threads(){
        super();
        *//*total_cpu_time = 0;
        total_wait_time = 0;
        previous_cpu_time = this.getTimeOnCPU();
        previous_waiting_time = 0;*//*
        this.time_added_to_ready_queue = 0;
        this.time_removed_from_ready_queue = 0;
        this.total_wait_time = 0;
    }

    long getTime_added_to_ready_queue(){
        return time_added_to_ready_queue;
    }

    long getTime_removed_from_ready_queue(){
        return time_removed_from_ready_queue;
    }

    void setTime_added_to_ready_queue(long time_added_to_ready_queue){
        this.time_added_to_ready_queue = time_added_to_ready_queue;
    }

    void setTime_removed_from_ready_queue(long time_removed_from_ready_queue){
        this.time_removed_from_ready_queue = time_removed_from_ready_queue;
    }
    long getTotal_wait_time(){
        total_wait_time = total_wait_time + time_removed_from_ready_queue - time_added_to_ready_queue;
        return total_wait_time;
    }


}*/

