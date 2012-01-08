package com.ichi2.anki.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteOpenMode;

public class TestSQLDB {

	/*
	public static void main(String[] args) {
		
		SQLiteConfig config = new SQLiteConfig();
		config.setOpenMode(SQLiteOpenMode.READWRITE);
		
		SQLiteDataSource mDatabase = new SQLiteDataSource(config);
		mDatabase.setUrl("jdbc:sqlite:Recettes.anki");
		
		if (mDatabase != null) {
			try {
				boolean walMode = true;

				if (walMode) {
					Connection conn = null;
					try {
						conn = mDatabase.getConnection();
						Statement stat = conn.createStatement();
						stat.execute("PRAGMA journal_mode = WAL");
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						if (conn != null) {
							try {
								conn.close();
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				} else {
					mDatabase.setJournalMode("DELETE");
				}
				
				// FIXME #URGENT !!
				boolean asyncMode = false;
				// FIXME #URGENT !!
				
				if (asyncMode) {
					mDatabase.setSynchronous("OFF");
					//cur = mDatabase.rawQuery("PRAGMA synchronous = 0", null);
				} else {
					mDatabase.setSynchronous("FULL");
					//cur = mDatabase.rawQuery("PRAGMA synchronous = 2", null);
				}

			} finally {
				
			}
		} 
	}*/
}
