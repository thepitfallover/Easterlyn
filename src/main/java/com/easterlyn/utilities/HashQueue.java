package com.easterlyn.utilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A Queue which ensures that elements are unique.
 * 
 * @author Jikoo
 */
public class HashQueue<E> implements Queue<E> {

	private final Queue<E> queue = new LinkedList<>();
	private final Set<E> set = new HashSet<>();

	@Override
	public int size() {
		return queue.size();
	}

	@Override
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private final Iterator<E> queueIterator = queue.iterator();
			@Override
			public boolean hasNext() {
				return queueIterator.hasNext();
			}

			@Override
			public E next() {
				return queueIterator.next();
			}
		};
	}

	@Override
	public Object[] toArray() {
		return queue.toArray();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] toArray(Object[] a) {
		return queue.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		return set.remove(o) && queue.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean addAll = true;
		for (E e : c) {
			addAll = add(e) && addAll;
		}
		return addAll;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c) || queue.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c) || queue.retainAll(c);
	}

	@Override
	public void clear() {
		set.clear();
		queue.clear();
	}

	@Override
	public boolean add(E e) {
		return set.add(e) && queue.add(e);
	}

	@Override
	public boolean offer(E e) {
		return set.add(e) && queue.add(e);
	}

	@Override
	public E remove() {
		E e = queue.remove();
		set.remove(e);
		return e;
	}

	@Override
	public E poll() {
		E e = queue.poll();
		set.remove(e);
		return e;
	}

	@Override
	public E element() {
		return queue.element();
	}

	@Override
	public E peek() {
		return queue.peek();
	}

}
