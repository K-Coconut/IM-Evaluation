package com.cityu.im;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Ethan
 * @date 2021/6/11 1:52 上午
 */
public class Graph {
    public HashMap<Integer, HashMap<Integer, Float>> nodes = new HashMap<>();
    public Integer n;
    public Integer m;
    public boolean[] visited;
    public int[] degree;
    private static Logger log = Logger.getLogger(GenerateRRSets.class.getName());

    private void readAttri(String path) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
        this.n = Integer.parseInt(bufferedReader.readLine().split("n=")[1]);
        this.m = Integer.parseInt(bufferedReader.readLine().split("m=")[1]);
    }

    public void readGraph(String path) throws IOException {
        readAttri(path + "attribute.txt");
        File file = new File(path + "/edges.txt");
        int m = 0;
        if (file.isFile() && file.exists()) {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file));
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineText = null;
            int cnt = 1;
            int print_every = this.m / 10;
            while ((lineText = bufferedReader.readLine()) != null) {
                if (cnt % print_every == 0) {
                    log.info(String.format("Reading Graph: %d/%d [%.2f]%%", cnt, this.m, (100.0 * cnt) /  this.m));
                }
                cnt ++;
                if (lineText.startsWith("#")) continue;
                String[] split = lineText.strip().replace('\t', ' ').split(" ");
                if (split.length < 3) break;
                nodes.putIfAbsent(Integer.parseInt(split[0]), new HashMap<>());
                nodes.putIfAbsent(Integer.parseInt(split[1]), new HashMap<>());
                nodes.get(Integer.parseInt(split[0])).put(Integer.parseInt(split[1]), Float.parseFloat(split[2]));
                nodes.get(Integer.parseInt(split[1])).put(Integer.parseInt(split[0]), Float.parseFloat(split[2]));
                m += 1;
            }
        }
        assert this.n != this.nodes.size() : "n != number of nodes";
        assert this.m != m : "m != number of edges";
        this.visited = new boolean[this.n];
        this.degree = new int[this.n];

    }

    public void generateRRSet(int source, RRSets S) {
        HashSet<Integer> visitedNodes = new HashSet<>();
        ArrayList<Integer> activeNodes = new ArrayList<>();
        int front = 0;
        int rear = 0;

        activeNodes.add(source);
        visitedNodes.add(source);
        rear++;
        while (front < rear) {
            int seed = activeNodes.get(front);
            front++;

            for (Map.Entry<Integer, Float> pair : this.nodes.get(seed).entrySet()) {
                int neighbor = pair.getKey();
                float probability = pair.getValue();
                if (visitedNodes.contains(neighbor)) {
                    continue;
                }
                if (Math.random() < probability) {
                    activeNodes.add(neighbor);
                    visitedNodes.add(neighbor);
                    rear++;
                }
            }
        }
        activeNodes.trimToSize();
        S.hyperGT.add(activeNodes);

        for (int node : activeNodes) {
            S.hyperG.putIfAbsent(node, new ArrayList<>());
            S.hyperG.get(node).add(S.hyperGT.size() - 1);
        }
    }

    public void buildRRIndex(RRSets S) {
        int rrId = 0;
        for (List<Integer> set : S.hyperGT) {
            for (int node : set) {
                S.hyperG.putIfAbsent(node, new ArrayList<>());
                S.hyperG.get(node).add(rrId);
            }
            rrId += 1;
        }
    }
}

class RRSets {
    public ArrayList<ArrayList<Integer>> hyperGT;
    public HashMap<Integer, ArrayList<Integer>> hyperG;

    public RRSets() {
        this.hyperGT = new ArrayList<>();
        this.hyperG = new HashMap<>();
    }

    public void trim() {
        for (Map.Entry<Integer, ArrayList<Integer>> set : hyperG.entrySet()) {
            set.getValue().trimToSize();
        }
        this.hyperGT.trimToSize();
    }
}