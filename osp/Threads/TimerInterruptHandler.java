/**
 * @Author Weifeng Lin
 * @StudentID 110161112
 */
package osp.Threads;

import osp.IFLModules.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**    
       The timer interrupt handler.  This class is called upon to
       handle timer interrupts.

       @OSPProject osp.Threads
*/
public class TimerInterruptHandler extends IflTimerInterruptHandler
{
    /**
       This basically only needs to reset the times and dispatch
       another process.

       @OSPProject osp.Threads
    */
    public void do_handleInterrupt()
    {
        // your code goes here
        ThreadCB.dispatch();

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/


