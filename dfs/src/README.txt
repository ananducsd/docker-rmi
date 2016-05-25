--------------
Group Members
--------------

Name	: Akanksha Maurya
PID 	: A53087643
Email 	: amaurya@eng.ucsd.edu

Name	: Fnu Anand
PID 	: A53081794
Email 	: a3anand@cs.ucsd.edu

Name	: Siddhant Arya
PID 	: A53079389
Email	: sarya@eng.ucsd.edu

------------------------------------------------------------------------------------
RMI implementation

We are using reference-rmi.jar (provided by the staff) as our rmi implementation. We have edited the makefile to include reference-rmi.jar in the classpath.

------------------------------------------------------------------------------------
Locks and path ordering

Our implementation of the lock method relies on creating a custom <code>CustomReadWriteLock</code> lock.
This lock does not support re-entrancy but merely maintains a counter for readers and writers, and
write requests.
   	 
The CustomReadWriteLock has two lock methods and two unlock methods. One lock
and unlock method for read access and one lock and unlock for write
access.

The protocol for read access is implemented in the CustomReadWriteLock.lockRead() method. All
threads get read access unless there is a thread with write access, or
one or more threads have requested write access. This places a write request at a higher priority
than read requests, which under a normal workload(more reads/ fewer writes) should prevent starvation.

The rules for write access are implemented in the CustomReadWriteLock.lockWrite() method. A
thread that needs write access first increments the counter for writeRequests (writeRequests++). 
Then it checks if it can actually get write access. A thread can get write access if there are no 
threads with read or write access to the resource.

To avoid deadlocks, we need to impose a total topological ordering
on the sequence of lock requests. We equate this ordering to the lexicographical 
ordering of absolute path names. A dictionary order also satisfies the requirement
that parent directories are locked prior to requesting access to any child 
subdirectories or files. Thus, while locking a path object, shared locks are 
requested for all the parent objects leading to the path - which satisfies the 
lexical order. If well-behaved want simultaneous access to multiple paths, it is 
their responsibility to order their access requests using this <code>compareTo</code>
method. Thus, in the above example, the first application will request lock on 
<code>/bin/cat</code> and not <code>/etc</code>. Whoever gets the lock first on / (root)
will win and the operation will go through. The other application will wait to acquire 
the lock, and thus deadlock will be avoided. 


