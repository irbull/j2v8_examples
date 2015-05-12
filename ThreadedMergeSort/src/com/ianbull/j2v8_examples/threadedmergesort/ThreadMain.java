/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.ianbull.j2v8_examples.threadedmergesort;

import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.utils.V8ObjectUtils;

public class ThreadMain {
	
    private List<Object> mergeSortResults = new ArrayList<Object>();

	private static final String sortAlgorithm = "" 
			+ "function merge(left, right){\n" 
			+ "  var result  = [],\n"
			+ "  il      = 0,\n" 
			+ "  ir      = 0;\n" 
			+ "  while (il < left.length && ir < right.length){\n"
			+ "    if (left[il] < right[ir]){\n" 
			+ "      result.push(left[il++]);\n" 
			+ "    } else {\n"
			+ "      result.push(right[ir++]);\n" 
			+ "    }\n" + "  }\n"
			+ "  return result.concat(left.slice(il)).concat(right.slice(ir));\n" 
			+ "};\n"
			+ "\n"
			+ "function sort(data) {\n" 
			+ "  if ( data.length === 1 ) {\n" 
			+ "    return [data[0]];\n"
			+ "  } else if (data.length === 2 ) {\n"
			+ "    if ( data[1] < data[0] ) {\n"
			+ "      return [data[1],data[0]];\n" 
			+ "    } else {\n" 
			+ "      return data;\n"
			+ "    }\n" 
			+ "  }\n"
			+ "  var mid = Math.floor(data.length / 2);\n" 
			+ "  var first = data.slice(0, mid);\n"
			+ "  var second = data.slice(mid);\n" 
			+ "  return merge(_sort( first ), _sort( second ) );\n" 
			+ "}\n";

	public class Sort implements JavaCallback {
		List<Object> result = null;

		public Object invoke(final V8Array parameters) {
			final List<Object> data = V8ObjectUtils.toList(parameters);

			Thread t = new Thread(new Runnable() {

				public void run() {
					V8 runtime = V8.createV8Runtime();
					runtime.registerJavaMethod(new Sort(), "_sort");
					runtime.executeVoidScript(sortAlgorithm);
					V8Array parameters = V8ObjectUtils.toV8Array(runtime, data);
					V8Array _result = runtime.executeArrayFunction("sort", parameters);
					result = V8ObjectUtils.toList(_result);
					_result.release();
					parameters.release();
					runtime.release();
				}
			});
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return V8ObjectUtils.toV8Array(parameters.getRutime(), result);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		new ThreadMain().testMultiV8Threads();
	}
	
	public void testMultiV8Threads() throws InterruptedException {
		int totalThreads = 4;
		final List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < totalThreads; i++) {
			Thread t = new Thread(new Runnable() {

				public void run() {
					V8 v8 = V8.createV8Runtime();
					v8.registerJavaMethod(new Sort(), "_sort");
					v8.executeVoidScript(sortAlgorithm);
					V8Array data = new V8Array(v8);
					int max = 100;
					for (int i = 0; i < max; i++) {
						data.push(max - i);
					}
					V8Array parameters = new V8Array(v8).push(data);
					V8Array result = v8.executeArrayFunction("sort", parameters);
					synchronized (threads) {
						mergeSortResults.add(V8ObjectUtils.toList(result));
					}
					result.release();
					parameters.release();
					data.release();
					v8.release();
				}
			});
			threads.add(t);
		}
		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}
		
		for (int i = 0; i < totalThreads; i++) {
			List<Integer> result = (List<Integer>) mergeSortResults.get(i);
			for ( int j = 0; j < result.size(); j++) {
				System.out.print(result.get(j) + " ");
			}
			System.out.println();
		}
	}

}
