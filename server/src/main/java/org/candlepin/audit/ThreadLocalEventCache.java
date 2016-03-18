package org.candlepin.audit;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Cache of Events that is local to a thread. 
 * 
 * @author fnguyen
 *
 */
public class ThreadLocalEventCache implements Iterable<Event> {

    private ThreadLocal<LinkedList<Event>> localList = new ThreadLocal<LinkedList<Event>>();
    
    public void addLast(Event e) {
        LinkedList<Event> list = localList.get();
        if (list == null)
            localList.set(new LinkedList<Event>());
        localList.get().addLast(e);
    }

    public void clear() {
        localList.get().clear();
    }

    @Override
    public Iterator<Event> iterator() {
        return localList.get().iterator();
    }

}
