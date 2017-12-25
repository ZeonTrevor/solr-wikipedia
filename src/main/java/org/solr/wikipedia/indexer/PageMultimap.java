package org.solr.wikipedia.indexer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.Validate;
import org.solr.wikipedia.model.Page;
import org.solr.wikipedia.model.Revision;
import org.solr.wikipedia.model.Contributor;

import java.util.UUID;

/**
 * Produces the Multimap of key/value pairs for a given Page to index in Solr.
 *
 * @author bryanbende
 */
public class PageMultimap {

    private Page page;

    public PageMultimap(Page page) {
        this.page = page;
        Validate.notNull(page);
    }

    public Multimap<String,Object> getMultimap() {
        Multimap<String,Object> multimap = HashMultimap.create();
        //multimap.put(IndexField.id.name(), UUID.nameUUIDFromBytes(
        //        page.getTitle().getBytes()));
        multimap.put(IndexField.id.name(), page.getId());
        multimap.put(IndexField.TITLE.name(), page.getTitle());
        multimap.put("redirect", page.isRedirect());
        for(Revision rev : page.getRevisions()) {
            multimap.put(IndexField.REVISION_TIMESTAMP.name(), rev.getTimestamp());
            multimap.put(IndexField.REVISION_TEXT.name(), rev.getText());
            /*for(Contributor cont : rev.getContributors()) {
            	multimap.put(IndexField.CONTRIBUTOR_ID.name(), cont.getId());
            	if (cont.getUsername() != null) {
            		multimap.put(IndexField.CONTRIBUTOR_USERNAME.name(), cont.getUsername());
            	}
            }*/
        }

        return multimap;
    }

}
