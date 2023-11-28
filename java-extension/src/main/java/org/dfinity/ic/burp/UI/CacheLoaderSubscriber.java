package org.dfinity.ic.burp.UI;

public class CacheLoaderSubscriber {
    TopPanel delegate;

    public void onCacheLoad(){
        if(delegate != null){
        delegate.onCacheLoad();
        }
    }

    public void setDelegate(TopPanel delegate){
        this.delegate = delegate;
    }


}
