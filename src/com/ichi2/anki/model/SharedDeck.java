/***************************************************************************************
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki.model;


import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedDeck extends HashMap<String, Object> {

	private Logger log = LoggerFactory.getLogger(SharedDeck.class);
	
    private static final long serialVersionUID = 1L;
    
    private int mId;
    private String mUsername;
    private String mTitle;
    private String mDescription;
    private String mTags;
    private int mVersion;
    private int mFacts;
    private int mSize;
    private int mCount;
    private double mModified;
    private String mFileName;
    
    /**
     * on demand cache for filtering only
     */
    private String mLowerCaseTitle;


    public int getId() {
        return mId;
    }


    public void setId(int id) {
        mId = id;
    }


    public void setUsername(String username) {
        mUsername = username;
    }


    public String getTitle() {
        return mTitle;
    }


    public void setTitle(String title) {
        mTitle = title;
        put("title", mTitle);
    }


    public void setDescription(String description) {
        mDescription = description;
    }


    public void setTags(String tags) {
        mTags = tags;
    }


    public void setVersion(int version) {
        mVersion = version;
    }


    public int getFacts() {
        return mFacts;
    }


    public void setFacts(int facts) {
        mFacts = facts;
        // FIXME: Localized or move
        if (facts == 1) {
            put("facts", mFacts + " " + "fact");
        } else {
            put("facts", mFacts + " " + "facts");
        }
    }


    public int getSize() {
        return mSize;
    }


    public void setSize(int size) {
        mSize = size;
    }


    public void setCount(int count) {
        mCount = count;
    }


    public void setModified(double modified) {
        mModified = modified;
    }


    public void setFileName(String fileName) {
        mFileName = fileName;
    }


    public void prettyLog() {
        log.info("SHARED DECK:");
        log.info("        username = " + mUsername);
        log.info("        title = " + mTitle);
        log.info("        description = " + mDescription);
        log.info("        tags = " + mTags);
        log.info("        version = " + mVersion);
        log.info("        facts = " + mFacts);
        log.info("        size = " + mSize);
        log.info("        count = " + mCount);
        log.info("        modified = " + mModified);
        log.info("        fileName = " + mFileName);
    }


	public boolean matchesLowerCaseFilter(String searchText) {
		// cache our own lower case title, so the next letters in the filter string will be faster
		if (mLowerCaseTitle == null) {
			mLowerCaseTitle = getTitle().toLowerCase();
		}
		return mLowerCaseTitle.contains(searchText);
	}
}
