import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class AnalyseBigram {

	// a comparator using generic type
	private class ValueComparator<K extends Comparable<K>, V extends Comparable<V>> implements Comparator<K>{
	 
		HashMap<K, V> map = new HashMap<K, V>();
	 
		public ValueComparator(HashMap<K, V> map){
			this.map.putAll(map);
		}
	 
		@Override
		public int compare(K s1, K s2) {
			int val = -map.get(s1).compareTo(map.get(s2));//descending order
			if(val != 0) return val;
			else return s1.compareTo(s2);
		}
	}
	
	public static void main(String[] args) {
		
		if(args.length < 1) {
			System.out.println("Usage: java AnalyseBigram <file-name>");
			return;
		}
		
		String fileName = args[0];
		BufferedReader br = null;
		
		HashMap<String, Integer> m = new HashMap<String, Integer>();
		
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line = null;
			long total = 0;
			while ( (line = br.readLine()) != null) {
				String[] terms = line.split("[ \t]");
				String bigram = terms[0] + " " + terms[1];
				int count = Integer.parseInt(terms[2]);
				m.put(bigram, count);
				total += count;
			}
			
			Comparator<String> comp = new AnalyseBigram().new ValueComparator<String, Integer>(m);
			TreeMap<String,Integer> treeMap = new TreeMap<String,Integer>(comp);
			treeMap.putAll(m);
			//System.out.println(treeMap);
			long threshold = Math.round(0.1 * total);
			
			StringBuffer out = new StringBuffer();
			out.append("--------------------------------------------------------------------------\n");
			out.append("Total number of bigrams = " + total + "\n");
			out.append("Most Common bigram = " + treeMap.firstEntry().getKey() + "\n");
			
			long count = 0, bigramCount = 0;
			for(Entry<String, Integer> e : treeMap.entrySet()){
				count += e.getValue();
				bigramCount++;
				if(count > threshold) break;
			}
			out.append("Number of bigrams required to add up to 10% of all bigrams = " + bigramCount + "\n");
			out.append("--------------------------------------------------------------------------\n");
			System.out.println(out.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch(Exception e) {
				
			}
		}
		
	}

}

