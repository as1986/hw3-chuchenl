package edu.cmu.lti.f14.hw3.casconsumers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.typesystems.Document;
import edu.cmu.lti.f14.hw3.typesystems.Token;
import edu.cmu.lti.f14.hw3.utils.Utils;
import edu.stanford.nlp.util.Sets;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  class MyDocument {
    /**
     * constructs pojo document object
     * 
     * @param doc
     */
    public MyDocument(Document doc) {
      termVec = new HashMap<String, Integer>();
      text = doc.getText();
      List<Token> tokenList = Utils.fromFSListToCollection(doc.getTokenList(), Token.class);
      for (Token t : tokenList) {
        termVec.put(t.getText(), t.getFrequency());
      }
      relevant = doc.getRelevanceValue();
    }

    Map<String, Integer> termVec;

    String text;

    int relevant;

    public double getCosSimilarity(MyDocument other) {
      return computeCosineSimilarity(other.termVec, this.termVec);
    }
  }

  public List<List<Token>> termVecList;

  Map<Integer, List<MyDocument>> documents;

  Map<Integer, MyDocument> queries;

  public void initialize() throws ResourceInitializationException {

    termVecList = new ArrayList<List<Token>>();

    documents = new HashMap<Integer, List<MyDocument>>();
    queries = new HashMap<Integer, MyDocument>();

  }

  /**
   * 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator<?> it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

      if (doc.getRelevanceValue() == 99) {
        queries.put(doc.getQueryID(), new MyDocument(doc));
      } else {
        if (!documents.containsKey(doc.getQueryID())) {
          documents.put(doc.getQueryID(), new ArrayList<MyDocument>());
        }
        documents.get(doc.getQueryID()).add(new MyDocument(doc));
      }
      termVecList.add(tokenList);
    }

  }

  /**
   * 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    PrintWriter writer = new PrintWriter("report.txt", "UTF-8");

    List<Integer> queryIds = new ArrayList<Integer>(queries.keySet());
    List<Integer> rankings = new ArrayList<Integer>();
    Collections.sort(queryIds);
    // computes ranking and cos similarity
    for (Integer q : queryIds) {
      MyDocument queryDoc = queries.get(q);
      List<MyDocument> ranked = new ArrayList<MyDocument>(documents.get(q));
      int retrieved = computeRank(ranked, queryDoc);
      rankings.add(retrieved + 1);
      String output = String.format("cosine=%.4f\trank=%d\tqid=%d\trel=%d\t%s",
              ranked.get(retrieved).getCosSimilarity(queryDoc), retrieved + 1, q,
              ranked.get(retrieved).relevant, ranked.get(retrieved).text);
      writer.println(output);
      writer.println("best: " + ranked.get(0).getCosSimilarity(queryDoc) + "\ttext: "
              + ranked.get(0).text);
    }

    // computes MRR
    double metric_mrr = compute_mrr(rankings);
    writer.println(String.format("MRR=%.4f", metric_mrr));
    writer.close();
  }

  /**
   * computes rank given list of documents and query
   * 
   * @param toBeRanked
   * @param query
   * @return
   */
  private int computeRank(List<MyDocument> toBeRanked, final MyDocument query) {
    Collections.sort(toBeRanked, new Comparator<MyDocument>() {
      @Override
      public int compare(MyDocument o1, MyDocument o2) {
        Double c1 = new Double(o1.getCosSimilarity(query));
        Double c2 = new Double(o2.getCosSimilarity(query));
        return -c1.compareTo(c2);
      }
    });
    for (int i = 0; i < toBeRanked.size(); i++) {
      if (toBeRanked.get(i).relevant == 1)
        return i;
    }
    throw new IllegalArgumentException("no relevant document found!");

  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    Set<String> inter = Sets.intersection(queryVector.keySet(), docVector.keySet());
    double unnormalized = 0;
    for (String interKey : inter) {
      unnormalized += queryVector.get(interKey) * docVector.get(interKey);
    }

    double queryNorm = 0;
    for (Entry<String, Integer> entry : queryVector.entrySet()) {
      queryNorm += entry.getValue() * entry.getValue();
    }

    double docNorm = 0;
    for (Entry<String, Integer> entry : docVector.entrySet()) {
      docNorm += entry.getValue() * entry.getValue();
    }

    cosine_similarity = unnormalized / (Math.sqrt(queryNorm) * Math.sqrt(docNorm));

    return cosine_similarity;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr(List<Integer> rankings) {
    double metric_mrr = 0.0;
    for (int r : rankings) {
      double inv = 1. / r;
      metric_mrr += inv;
    }
    metric_mrr /= rankings.size();

    return metric_mrr;
  }

}
