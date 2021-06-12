package com.cityu.im;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * @author Ethan
 * @date 2021/6/11 1:18 上午
 */
public class GenerateRRSets {
    public Graph graph = new Graph();
    private final String basePath = "/Users/coconut/code/git/research/GCOMB-Research/IM/IM_TV/GraphSAGE-master/real_data/%s/TV/test/";
    private static Logger log = Logger.getLogger(GenerateRRSets.class.getName());

    public static void generate(String dataset, int num) throws IOException {
        GenerateRRSets inf = new GenerateRRSets();
        log.info(String.format("Loading com.cityu.im.Graph %s", dataset));
        inf.readData(dataset);
        log.info(String.format("com.cityu.im.Graph loaded, n=%d, m=%d", inf.graph.n, inf.graph.m));
        log.info(String.format("To Generate %d RR Sets", num));
        Long st = System.currentTimeMillis();
        RRSets S = inf.generateRRSets(num);
        Long ed = System.currentTimeMillis();
        log.info(String.format("%d RR Sets generated, takes %fs", num, (ed - st) / 1000.));
        inf.dumpRRSets(S, dataset, num);
    }

    public void readData(String dataset) throws IOException {
        String path = String.format(this.basePath, dataset);
        this.graph.readGraph(path);
    }

    public RRSets generateRRSets(int num) {
        RRSets S = new RRSets();
        for (int i = 0; i < num; i++) {
            int source = (int) (Math.random() * graph.n);
            this.graph.generateRRSet(source, S);
        }
        this.graph.buildRRIndex(S);
        return S;
    }

    public void dumpRRSets(RRSets S, String dataset, int num) throws IOException {
        String jsonString = JSON.toJSONString(S);
        String dirPath = String.format(this.basePath, dataset) + "/large_graph/mc_RR_Sets/";
        boolean mkdirs = new File(dirPath).mkdirs();
        if (mkdirs) System.out.printf("Create Directory: %s", dirPath);
        String file = dirPath + String.format("RR%d", num);
        FileOutputStream fileOut = new FileOutputStream(new File(file));
        OutputStreamWriter osw = new OutputStreamWriter(fileOut, StandardCharsets.UTF_8);
        osw.write(jsonString);
        osw.flush();
        osw.close();
        log.info(String.format("com.cityu.im.RRSets dumped to %s", file));
    }

}