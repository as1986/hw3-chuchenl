package edu.cmu.lti.f14.hw3.annotators;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.typesystems.Document;
import edu.cmu.lti.f14.hw3.typesystems.Token;
import edu.cmu.lti.f14.hw3.utils.Utils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   * 
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  List<String> tokenize1(String doc) {
    PTBTokenizer tokenizer = new PTBTokenizer(new StringReader(doc), new CoreLabelTokenFactory(),
            "");
    List<String> toReturn = new ArrayList<String>();
    for (CoreLabel label; tokenizer.hasNext();) {
      label = (CoreLabel) tokenizer.next();
      toReturn.add(label.value());
    }
    return toReturn;
  }

  /**
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();

    // uncomment below for Stanford CoreNLP tokenizer
    // List<String> tokenized = tokenize1(docText);
    List<String> tokenized = tokenize0(docText);
    Map<String, Integer> counter = new HashMap<String, Integer>();
    for (String t : tokenized) {
      // uncomment below to use Stanford CoreNLP lemmatizer
      // String lemma = StanfordLemmatizer.stemWord(t.toLowerCase());
      String lemma = t;
      if (counter.containsKey(lemma)) {
        counter.put(t, counter.get(lemma) + 1);
      } else {
        counter.put(lemma, 1);
      }
    }
    List<Token> tokens = new ArrayList<Token>();
    for (String s : counter.keySet()) {
      Token tk = new Token(jcas);
      tk.setText(s);
      tk.setFrequency(counter.get(s));
      tk.addToIndexes();
      tokens.add(tk);
    }
    FSList tokenList = Utils.fromCollectionToFSList(jcas, tokens);
    doc.setTokenList(tokenList);
    tokenList.addToIndexes();
  }

}
