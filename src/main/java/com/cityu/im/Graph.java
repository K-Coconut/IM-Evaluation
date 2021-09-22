package com.cityu.im;

import java.io.*;
import java.util.*;

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
            while ((lineText = bufferedReader.readLine()) != null) {
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
        List<Integer> activeNodes = new ArrayList<>(Arrays.asList(source)), seeds = new ArrayList<>(Arrays.asList(source)), visitedNodes = new ArrayList<>(Arrays.asList(source));

        this.visited[source] = true;
        while (!seeds.isEmpty()) {
            List<Integer> newSeeds = new ArrayList<>();
            for (int seed : seeds) {
                for (int neighbor : this.nodes.get(seed).keySet()) {
                    if (this.visited[neighbor]) {
                        continue;
                    }
                    if (Math.random() < this.nodes.get(seed).get(neighbor)) {
                        this.visited[neighbor] = true;
                        visitedNodes.add(neighbor);
                        activeNodes.add(neighbor);
                        newSeeds.add(neighbor);
                    }
                }
            }
            seeds = newSeeds;
        }
        S.hyperGT.add(activeNodes);

        // reset
        for (int node : visitedNodes) {
            this.visited[node] = false;
        }
    }

    public List<Integer> lowMemoryGenerateRRSet(int source) {
        List<Integer> activeNodes = new ArrayList<>(Arrays.asList(source)), seeds = new ArrayList<>(Arrays.asList(source)), visitedNodes = new ArrayList<>(Arrays.asList(source));

        this.visited[source] = true;
        while (!seeds.isEmpty()) {
            List<Integer> newSeeds = new ArrayList<>();
            for (int seed : seeds) {
                for (int neighbor : this.nodes.get(seed).keySet()) {
                    if (this.visited[neighbor]) {
                        continue;
                    }
                    if (Math.random() < this.nodes.get(seed).get(neighbor)) {
                        this.visited[neighbor] = true;
                        visitedNodes.add(neighbor);
                        activeNodes.add(neighbor);
                        newSeeds.add(neighbor);
                    }
                }
            }
            seeds = newSeeds;
        }
        // reset
        for (int node : visitedNodes) {
            this.visited[node] = false;
        }
        return activeNodes;
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

class RRSets implements Serializable {
    public List<List<Integer>> hyperGT;
    public HashMap<Integer, List<Integer>> hyperG;

    public RRSets() {
        this.hyperGT = new ArrayList<>();
        this.hyperG = new HashMap<>();
    }
}