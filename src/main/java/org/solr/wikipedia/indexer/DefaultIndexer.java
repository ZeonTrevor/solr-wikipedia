package org.solr.wikipedia.indexer;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang3.Validate;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.Builder;
import org.apache.solr.common.SolrInputDocument;
import org.solr.wikipedia.handler.DefaultPageHandler;
import org.solr.wikipedia.iterator.SolrInputDocPageIterator;
import org.solr.wikipedia.iterator.WikiMediaIterator;
import org.solr.wikipedia.model.Page;

import javax.xml.stream.XMLStreamException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Indexes Pages in Solr.
 *
 * @author bryanbende
 */
public class DefaultIndexer {

    static final int DEFAULT_BATCH_SIZE = 40;

    private final int batchSize;
    private final HttpSolrClient solrServer;
    private final String redirectsFile;
    
    private List<String> redirectPageIds;
    
    /**
     *
     * @param solrServer
     */
    public DefaultIndexer(HttpSolrClient solrServer, String redirectsFile) {
        this(solrServer, redirectsFile, DEFAULT_BATCH_SIZE);
    }

    /**
     *
     * @param solrServer
     * @param batchSize
     */
    public DefaultIndexer(HttpSolrClient solrServer, String redirectsFile, int batchSize) {
        this.solrServer = solrServer;
        this.batchSize = batchSize;
        this.redirectsFile = redirectsFile;
        Validate.notNull(this.solrServer);
        if (this.batchSize <= 0) {
            throw new IllegalStateException("Batch size must be > 0");
        }
        //loadWikiRedirects();
    }

    /**
     * Iterates over docs adding each SolrInputDocument to the given SolrServer
     * in batches.
     *
     * @param docs
     * @throws IOException
     * @throws SolrServerException
     */
    public void index(Iterator<SolrInputDocument> docs) throws IOException, SolrServerException {
        if (docs == null) {
            return;
        }

        long count = 0;
        long redirectCount = 0;
        Collection<SolrInputDocument> solrDocs = new ArrayList<>();
        while(docs.hasNext()) {
            SolrInputDocument doc = docs.next();
            //String id = (String) doc.getFieldValue(IndexField.id.name());
            //if(!redirectPageIds.contains(id))
            boolean isRedirect = (boolean) doc.getFieldValue("redirect");
            if(!isRedirect)
            {	doc.removeField("redirect");
            	solrDocs.add(doc);
            }
            else
            	redirectCount++;

            if (solrDocs.size() >= this.batchSize) {
                count += solrDocs.size();
                System.out.println("reached batch size, total count = " + count + ", number of redirects skipped = " + redirectCount);
                solrServer.add(solrDocs);
                solrServer.commit();
                solrDocs.clear();
            }
        }

        if (solrDocs.size() > 0) {
        	count += solrDocs.size();
        	System.out.println("last batch, total count = " + count + ", number of redirects skipped = " + redirectCount);
            solrServer.add(solrDocs);
            solrServer.commit();
            solrDocs.clear();
        }
    }

	public void loadWikiRedirects()
	{
		redirectPageIds = new ArrayList<String>();
		try
		{
		String fileLocation = redirectsFile;//"files/wiki_redirects.txt.gz";
		GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(new File(fileLocation)));
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		String line = null;

		while((line = br.readLine()) != null)
			redirectPageIds.add(line);

		br.close();
		}
		catch(IOException e)
		{
			System.err.println("Error while loading wiki redirects list from file");
		}
		System.out.println("Total no. of wiki redirects page ids loaded: " + redirectPageIds.size());
	}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: DefaultIndexer <SOLR_URL> <WIKIPEDIA_DUMP_FILE> <REDIRECT_COMPRESSED_FILE>" +
                    "(<BATCH_SIZE>)");
            System.exit(0);
        }

        String solrUrl = args[0];
        String wikimediaDumpFile = args[1];
        //String redirectsFile = args[2];
        String redirectsFile ="";
        Validate.notEmpty(solrUrl);
        Validate.notEmpty(wikimediaDumpFile);
        //Validate.notEmpty(redirectsFile);
        
        // attempt to parse a provided batch size
        Integer batchSize = null;
        if (args.length == 3) {
            try {
                batchSize = Integer.valueOf(args[2]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try (FileInputStream fileIn = new FileInputStream(wikimediaDumpFile);
             BZip2CompressorInputStream bzipIn = new BZip2CompressorInputStream(fileIn);
             InputStreamReader reader = new InputStreamReader(bzipIn)) {

            Iterator<Page> pageIter = new WikiMediaIterator<>(
                    reader, new DefaultPageHandler());

            Iterator<SolrInputDocument> docIter =
                    new SolrInputDocPageIterator(pageIter);

            HttpSolrClient solrServer = new Builder(solrUrl).build();

            DefaultIndexer defaultIndexer = (batchSize != null ?
                    new DefaultIndexer(solrServer, redirectsFile, batchSize) :
                    new DefaultIndexer(solrServer, redirectsFile));

            long startTime = System.currentTimeMillis();

            defaultIndexer.index(docIter);

            System.out.println("Indexing finished at " + new Date());
            System.out.println("Took " + (System.currentTimeMillis() - startTime) + " ms");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (SolrServerException e) {
            e.printStackTrace();
        }
    }

}
