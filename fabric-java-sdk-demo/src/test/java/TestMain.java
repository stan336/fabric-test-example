import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class TestMain {
    public static void main(String[] args) {
        int i=1;
        int j=7;
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        HashMap<Integer,Integer> map1 = new HashMap<Integer,Integer>();
        while(i<100000)
        {
            Random random = new Random();
            Integer keyRandom = random.nextInt(10000000);
            int key = keyRandom % 8;
           // System.out.println(keyRandom);
            if(map.get(key)!=null){
               map.put(key,map.get(key)+1);
            }else{
                map.put(key,0);
            }
            map1.put(keyRandom,keyRandom);
            i++;
        }
        HashMap<Integer,Integer> map2 = new HashMap<Integer,Integer>();
            Set<Integer> keys = map1.keySet();
            // System.out.println(keyRandom);
            for(Integer key:keys) {
                int newkey = key % 10;
                if(map2.get(newkey)!=null){
                    map2.put(newkey,map2.get(newkey)+1);
                }else{
                    map2.put(newkey,0);
                }
            }
        System.out.println(map);
        System.out.println(map2);
    }
}
