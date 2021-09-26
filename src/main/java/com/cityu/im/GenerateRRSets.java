package com.cityu.im;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Ethan
 * @date 2021/6/11 1:18 上午
 */
public class GenerateRRSets {
    public Graph graph = new Graph();
    public String basePath = "";
    private static Logger log = Logger.getLogger(GenerateRRSets.class.getName());

    public static void generate(String dataset, int num, String basePath) throws IOException {
        GenerateRRSets inf = new GenerateRRSets();
        inf.basePath = basePath;
        log.info(String.format("Loading Graph %s", dataset));
        inf.readData(dataset);
        log.info(String.format("Graph loaded, n=%d, m=%d", inf.graph.n, inf.graph.m));
        log.info(String.format("To Generate %d RR Sets", num));
        Long st = System.currentTimeMillis();
        inf.generateRRSets(num);
        Long ed = System.currentTimeMillis();
        log.info(String.format("%d RR Sets generated, takes %fs", num, (ed - st) / 1000.));
    }

    public void readData(String dataset) throws IOException {
        String path = String.format(this.basePath, dataset);
        this.graph.readGraph(path);
    }

    public RRSets generateRRSets(int num) {
        RRSets S = new RRSets();
        for (int i = 1; i < num + 1; i++) {
            int source = (int) (Math.random() * this.graph.n);
            this.graph.generateRRSet(source, S);
            if (i % 1000 == 0) {
                log.info(String.format("[%d/%d] RR sets generated, Memory: %.2f M = %.3f G", i, num, (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024., (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024. / 1024.));
            }
        }
        this.graph.buildRRIndex(S);
        return S;
    }

    public void lowMemoryGenerateRRSets(String dataset, int num) throws IOException {
        String dirPath = String.format(this.basePath, dataset) + "/large_graph/mc_RR_Sets/";
        boolean mkdirs = new File(dirPath).mkdirs();
        if (mkdirs) System.out.printf("Create Directory: %s", dirPath);
        String file = dirPath + String.format("RR%d", num);
        FileOutputStream fileOut = new FileOutputStream(new File(file));
        OutputStreamWriter osw = new OutputStreamWriter(fileOut, StandardCharsets.UTF_8);

        int rrId = 0;
        HashMap<Integer, List<Integer>> hyperG = new HashMap<>();
        for (int i = 1; i < num + 1; i++) {
            int source = (int) (Math.random() * graph.n);
            List<Integer> rr = this.graph.lowMemoryGenerateRRSet(source);
            osw.write(rr.toString() + "\n");

            for (int node : rr) {
                hyperG.putIfAbsent(node, new ArrayList<>());
                hyperG.get(node).add(rrId);
            }
            rrId += 1;

            if (i % 1000 == 0) {
                log.info(String.format("[%d/%d] RR sets generated, Memory: %.2f M = %.3f G", i, num, (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024., (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024. / 1024.));
            }
        }

        osw.write("\n");
        for (Map.Entry<Integer, List<Integer>> set : hyperG.entrySet()) {
            osw.write(set.getKey() + ":" + set.getValue().toString() + "\n");
        }
        osw.flush();
        osw.close();
        log.info(String.format("RRSets dumped to %s", file));
    }

    public void dumpRRSets(RRSets S, String dataset, int num) throws IOException {
        String dirPath = String.format(this.basePath, dataset) + "/large_graph/mc_RR_Sets/";
        boolean mkdirs = new File(dirPath).mkdirs();
        if (mkdirs) System.out.printf("Create Directory: %s", dirPath);
        String file = dirPath + String.format("RR%d", num);
        FileOutputStream fileOut = new FileOutputStream(new File(file));
        OutputStreamWriter osw = new OutputStreamWriter(fileOut, StandardCharsets.UTF_8);
        for (List<Integer> rr : S.hyperGT) {
            osw.write(rr.toString() + "\n");
        }
        osw.write("\n");
        for (Map.Entry<Integer, List<Integer>> set : S.hyperG.entrySet()) {
            osw.write(set.getKey() + ":" + set.getValue().toString() + "\n");
        }
        osw.flush();
        osw.close();
        log.info(String.format("RRSets dumped to %s", file));
    }

}