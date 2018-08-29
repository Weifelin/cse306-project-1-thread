# cse306-project-1-thread
• Threads are scheduled according to their priority. Threads with the same priority are to
be scheduled according to the time they were inserted in the ready queue.

• The priority is calculated by the following expression: \
1.5 * total time the thread was waiting in the ready queue \
minus \
total CPU time the thread has used so far
minus \
0.3 * total CPU time all the threads in the same task have used so far.\

In many cases, the priority will be negative, as you can see.\
• When a thread is dispatched, it is given a CPU time slice of 100 time units. If the thread is
still running at the end of its time slice, it is preempted and put back into the ready queue.\
• If an interrupt occurs before the current thread finished its time slice, that thread has the
highest priority when the dispatcher is invoked next.\
• You must devise an organization for the ready queue by which scheduling will not require
scanning the entire queue in order to find the highest priority thread. This is not obvious
due to the third component in the priority formula
