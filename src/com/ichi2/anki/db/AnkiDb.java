/****************************************************************************************
 * Copyright (c) 2009 Daniel Svärd <daniel.svard@gmail.com>                             *
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Andrew <andrewdubya@gmail.com>                                    *
 *                                                                                      *
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

package com.ichi2.anki.db;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteOpenMode;

import com.ichi2.anki.model.Deck;
import com.ichi2.anki.utils.ConnectionAwarePreparedStatement;
import com.ichi2.anki.utils.ConnectionAwareResultSet;

/**
 * Database layer for AnkiDroid. Can read the native Anki format through
 * Android's SQLite driver.
 */
public class AnkiDb {

	public static Logger log = LoggerFactory.getLogger(AnkiDb.class);

	public enum SqlCommandType { SQL_INS, SQL_UPD, SQL_DEL };
	
	/**
	 * The deck, which is actually an SQLite database.
	 */
	//private SQLiteDatabase mDatabase;
	
	private SQLiteDataSource mDatabase;

	/**
	 * Open a database connection to an ".anki" SQLite file.
	 * @throws UnsupportedEncodingException 
	 */
	public AnkiDb(String ankiFilename, boolean forceDeleteJournalMode) throws UnsupportedEncodingException {
		
		SQLiteConfig config = new SQLiteConfig();
		config.setOpenMode(SQLiteOpenMode.READWRITE);
		
		mDatabase = new SQLiteDataSource(config);
		mDatabase.setUrl("jdbc:sqlite:" + ankiFilename);
		
		/* mDatabase = SQLiteDatabase.openDatabase(ankiFilename, null,
				SQLiteDatabase.OPEN_READWRITE
						| SQLiteDatabase.NO_LOCALIZED_COLLATORS); */
		if (mDatabase != null) {
			//Cursor cur = null;
			try {
				//String mode;
				// FIXME #URGENT !!
				boolean walMode = false;
				// FIXME #URGENT !!

				if (walMode) {
					Connection conn = null;
					try {
						conn = mDatabase.getConnection();
						Statement stat = conn.createStatement();
						stat.execute("PRAGMA journal_mode = WAL");
					} catch (SQLException e) {
						e.printStackTrace();
						return ;
					} finally {
						if (conn != null) {
							try {
								conn.close();
							} catch (SQLException e) {
							}
						}
					}
				} else {
					//mDatabase.setJournalMode("DELETE");
				}
				
				/*
				cur = mDatabase.rawQuery("PRAGMA journal_mode", null);
				if (cur.moveToFirst()) {
					String journalModeOld = cur.getString(1);
					cur.close();
					log.warn("Current Journal mode: " + journalModeOld);
					if (!journalModeOld.equalsIgnoreCase(mode)) {
						cur = mDatabase.rawQuery("PRAGMA journal_mode = "
								+ mode, null);
						if (cur.moveToFirst()) {
							String journalModeNew = cur.getString(1);
							cur.close();
							log.warn("Old journal mode was: " + journalModeOld
									+ ". Trying to set journal mode to " + mode
									+ ". Result: " + journalModeNew);
							if (journalModeNew.equalsIgnoreCase("wal")
									&& mode.equals("DELETE")) {
								log.error("Journal could not be changed to DELETE. Deck will probably be unreadable on sqlite < 3.7");
							}
						}
					}
				} */
				
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
				//cur.close();
				/*
				cur = mDatabase.rawQuery("PRAGMA synchronous", null);
				if (cur.moveToFirst()) {
					String syncMode = cur.getString(1);
					log.warn("Current synchronous setting: " + syncMode);
				}
				cur.close();
				*/
			} finally {
				/*
				if (cur != null && !cur.isClosed()) {
					cur.close();
				}
				*/
			}
		}
	}

	/**
	 * Closes a previously opened database connection.
	 */
	public void closeDatabase() {
		/*
		 * FIXME
		if (mDatabase != null) {
			mDatabase.close();
			log.info("AnkiDb - closeDatabase, database " + mDatabase.getPath()
					+ " closed = " + !mDatabase.isOpen());
			mDatabase = null;
		}
		*/
	}

	/*
	public SQLiteDatabase getDatabase() {
		return mDatabase;
	}
	*/

	/**
	 * Convenience method for querying the database for a single integer result.
	 * 
	 * @param query
	 *            The raw SQL query to use.
	 * @return The integer result of the query.
	 */
	public long queryScalar(String query) {
		//Cursor cursor = null;
		long scalar = -1;
		Connection conn = null;
		
		try {
			conn = mDatabase.getConnection();
			Statement stat = conn.createStatement();
			
			ResultSet result = stat.executeQuery(query);
			
			if (!result.next()) {
				throw new SQLException("No result for query: " + query);
			}
			
			scalar = result.getLong(1);
			
			/*
			cursor = mDatabase.rawQuery(query, null);
			if (!cursor.next()) {
				throw new SQLException("No result for query: " + query);
			}
			

			scalar = cursor.getLong(1); */
		} catch (SQLException e) {
			e.printStackTrace();
			scalar = -1;
		} finally {
			/*
			if (cursor != null) {
				cursor.close();
			}
			*/
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}

		return scalar;
	}

	/**
	 * Convenience method for querying the database for an entire column. The
	 * column will be returned as an ArrayList of the specified class. See
	 * Deck.initUndo() for a usage example.
	 * 
	 * @param type
	 *            The class of the column's data type. Example: int.class,
	 *            String.class.
	 * @param query
	 *            The SQL query statement.
	 * @param column
	 *            The column id in the result set to return.
	 * @return An ArrayList with the contents of the specified column.
	 */
	public <T> ArrayList<T> queryColumn(Class<T> type, String query, int column) {
		ArrayList<T> results = new ArrayList<T>();
		//Cursor cursor = null;

		Connection conn = null;
		try {
			conn = mDatabase.getConnection();
			Statement stat = conn.createStatement();
			
			ResultSet result = stat.executeQuery(query);
			
			// FXIME: Magical line ?
			//Array array = result.getArray(column);
			//ResultSet colResult = array.getResultSet();
			//array.getArray();
			
			while (result.next()) {
				results.add(type.cast(result.getObject(column)));
			}
			
			//cursor = mDatabase.rawQuery(query, null);
			
			//String methodName = getCursorMethodName(type.getSimpleName());
			//while (cursor.moveToNext()) {
				// The magical line. Almost as illegible as python code ;)
				
			//	results.add(type.cast(Cursor.class.getMethod(methodName,
			//			int.class).invoke(cursor, column)));
			//}
		//} catch (NoSuchMethodException e) {
			// This is really coding error, so it should be revealed if it ever
			// happens
		//	throw new RuntimeException(e);
		//} catch (IllegalArgumentException e) {
			// This is really coding error, so it should be revealed if it ever
			// happens
		//	throw new RuntimeException(e);
		//} catch (IllegalAccessException e) {
			// This is really coding error, so it should be revealed if it ever
			// happens
		//	throw new RuntimeException(e);
		//} catch (InvocationTargetException e) {
		//	throw new RuntimeException(e);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			/*if (cursor != null) {
				cursor.close();
			}*/
			
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}

		return results;
	}

	/**
	 * Method for executing db commands with simultaneous storing of undo
	 * information. This should only be called from undo method.
	 */
	public void execSQL(Deck deck, SqlCommandType command, String table, Map<String, Object> values, String whereClause) {
		if (command == SqlCommandType.SQL_INS) {
			insert(deck, table, null, values);
		} else if (command == SqlCommandType.SQL_UPD) {
			update(deck, table, values, whereClause);
		} else if (command == SqlCommandType.SQL_DEL) {
			delete(deck, table, whereClause);
		} else {
			log.info("wrong command. no action performed");
		}
	}

	/**
	 * Method for inserting rows into the db with simultaneous storing of undo
	 * information.
	 * 
	 * @return The id of the inserted row.
	 */
	public long insert(Deck deck, String table, String nullColumnHack, Map<String, Object> values) {
		
		Connection conn = null;
		try {
			conn = mDatabase.getConnection();
			Statement stat = conn.createStatement();
			
			String query = "INSERT INTO " + table + "(";
			
			for (Entry<String, Object> entry : values.entrySet()) {
				query += entry.getKey() + ",";
			}
			query = query.substring(0, query.length() - 1);
			query += ")";
			
			query += " VALUES (";
			for (Entry<String, Object> entry : values.entrySet()) {
				query +=  entry.getValue() + ",";
			}
			query = query.substring(0, query.length() - 1);
			query += ")";
			
			log.debug(query);
			
			stat.executeUpdate(query);
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
		
		// FIXME
		// WRONG
		long rowid = 1;
		
		//long rowid = mDatabase.insert(table, nullColumnHack, values);
		
		if (rowid != -1 && deck.recordUndoInformation()) {
			deck.addUndoCommand(SqlCommandType.SQL_DEL, table, null, "rowid = " + rowid);
		}
		return rowid;
	}

	/**
	 * Method for updating rows of the database with simultaneous storing of
	 * undo information.
	 * 
	 * @param values
	 *            A map from column names to new column values. Values must not
	 *            contain sql code/variables. Otherwise use update(Deck deck,
	 *            String table, Map<String, Object> values, String whereClause,
	 *            String[] whereArgs, boolean onlyFixedValues) with
	 *            'onlyFixedValues' = false.
	 * @param whereClause
	 *            The optional WHERE clause to apply when updating. Passing null
	 *            will update all rows.
	 * @param whereArgs
	 *            Arguments which will replace all '?'s of the whereClause.
	 */
	public void update(Deck deck, String table, Map<String, Object> values, String whereClause) {
		update(deck, table, values, whereClause, true);
	}

	/**
	 * Method for updating rows of the database with simultaneous storing of
	 * undo information.
	 * 
	 * @param values
	 *            A map from column names to new column values. null is a valid
	 *            value that will be translated to NULL.
	 * @param whereClause
	 *            The optional WHERE clause to apply when updating. Passing null
	 *            will update all rows.
	 * @param whereArgs
	 *            Arguments which will replace all '?'s of the whereClause.
	 * @param onlyFixedValues
	 *            Set this to true, if 'values' contains only fixed values (no
	 *            sql code). Otherwise, it must be set to false and fixed string
	 *            values have to be extra quoted ("\'example-value\'").
	 */
	public void update(Deck deck, String table, Map<String, Object> values, String whereClause, boolean onlyFixedValues) {
		update(deck, table, values, whereClause, onlyFixedValues, null, null);
	}

	public void update(Deck deck, String table, Map<String, Object> values, String whereClause, boolean onlyFixedValues,
			Map<String, Object>[] oldValuesArray, String[] whereClauseArray) {
		if (deck.recordUndoInformation()) {
			if (oldValuesArray != null) {
				for (int i = 0; i < oldValuesArray.length; i++) {
					deck.addUndoCommand(SqlCommandType.SQL_UPD, table, oldValuesArray[i],
							whereClauseArray[i]);
				}
			} else {
				ArrayList<String> ar = new ArrayList<String>();
				for (String key : values.keySet()) {
					ar.add(key);
				}
				int len = ar.size();
				String[] columns = new String[len + 1];
				ar.toArray(columns);
				columns[len] = "rowid";

				ResultSet result = null;
				try {
					result = query(table, columns, whereClause);
					while (result.next()) {
						Map<String, Object> oldvalues = new HashMap<String, Object>();
						for (int i = 0; i < len; i++) {
							// String typeName;
							// if (values.get(columns[i]) != null) {
							// typeName =
							// values.get(columns[i]).getClass().getSimpleName();
							// } else {
							// typeName = "String";
							// }
							// if (typeName.equals("String")) {
							// oldvalues.put(columns[i], cursor.getString(i));
							// } else if (typeName.equals("Long")) {
							// oldvalues.put(columns[i], cursor.getLong(i));
							// } else if (typeName.equals("Double")) {
							// oldvalues.put(columns[i], cursor.getDouble(i));
							// } else if (typeName.equals("Integer")) {
							// oldvalues.put(columns[i], cursor.getInt(i));
							// } else if (typeName.equals("Float")) {
							// oldvalues.put(columns[i], cursor.getFloat(i));
							// } else {
							oldvalues.put(columns[i], result.getString(i + 1));
							// }
						}
						deck.addUndoCommand(SqlCommandType.SQL_UPD, table, oldvalues,
								"rowid = " + result.getString(len + 1));
					}
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					if (result != null) {
						try {
							result.close();
						} catch (SQLException e) {
						}
					}
				}
			}
		}
		if (onlyFixedValues) {
			update(table, values, whereClause);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE ").append(table).append(" SET ");
			for (Entry<String, Object> entry : values.entrySet()) {
				sb.append(entry.getKey()).append(" = ")
						.append(entry.getValue()).append(", ");
			}
			sb.deleteCharAt(sb.length() - 2);
			sb.append("WHERE ").append(whereClause);
			execSQL(sb.toString());
		}
	}

	/**
	 * Method for deleting rows of the database with simultaneous storing of
	 * undo information.
	 */
	public void delete(Deck deck, String table, String whereClause) {
		if (deck.recordUndoInformation()) {
			ArrayList<String> columnsNames = new ArrayList<String>();
			// ArrayList<String> columnTypes = new ArrayList<String>();
			ResultSet result = null;

			try {
				result = rawQuery("PRAGMA TABLE_INFO(" + table + ")");
				while (result.next()) {
					columnsNames.add(result.getString(1));
					// String t = cursor.getString(2).toLowerCase();
					// String typeName = "";
					// if (t.subSequence(0, 3).equals("int")) {
					// typeName = "Long";
					// } else if (t.equals("float")) {
					// typeName = "Double";
					// } else {
					// typeName = "String";
					// }
					// columnTypes.add(typeName);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				if (result != null) {
					try {
						result.close();
					} catch (SQLException e) {
					}
				}
			}

			int len = columnsNames.size();
			String[] columns = new String[len];
			columnsNames.toArray(columns);

			try {
				result = query(table, columns, whereClause);
				while (result.next()) {
					Map<String, Object> oldvalues = new HashMap<String, Object>();
					for (int i = 0; i < len; i++) {
						// String typeName = columnTypes.get(i);
						// if (typeName.equals("String")) {
						// oldvalues.put(columns[i], cursor.getString(i));
						// } else if (typeName.equals("Long")) {
						// oldvalues.put(columns[i], cursor.getLong(i));
						// } else if (typeName.equals("Double")) {
						// oldvalues.put(columns[i], cursor.getDouble(i));
						// } else if (typeName.equals("Integer")) {
						// oldvalues.put(columns[i], cursor.getInt(i));
						// } else if (typeName.equals("Float")) {
						// oldvalues.put(columns[i], cursor.getFloat(i));
						// } else {
						oldvalues.put(columns[i], result.getString(i + 1));
						// }
					}
					deck.addUndoCommand(SqlCommandType.SQL_INS, table, oldvalues, null);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				if (result != null) {
					try {
						result.close();
					} catch (SQLException e) {
					}
				}
			}
		}
		delete(table, whereClause);
	}

	public ResultSet rawQuery(String query) {
		Connection conn = null;
		ResultSet result = null;
		
		try {
			conn = mDatabase.getConnection();
			Statement stat = conn.createStatement();
			
			result = stat.executeQuery(query);

		} catch (SQLException e) {
			log.error("Raw Query failed :", e);
			return null;
		}
		
		return new ConnectionAwareResultSet(result, conn);
	}
	
	public ResultSet query(String table, String columns[], String whereClause) {
	
		if (StringUtils.isBlank(table)) {
			log.error("Error in Query: table can not be blank");
			return null;
		}
		
		String query = "SELECT ";
		if (columns != null && columns.length > 0) {
			query += columns[0];
			for (int i = 1; i < columns.length; i++) {
				query += ", " + columns[i];
			}
		} else {
			query += "*";
		}
		query += " FROM " + table;
		if (StringUtils.isNotBlank(whereClause)) {
			query += " WHERE " + whereClause;
		}
		
		return rawQuery(query);
	}
	
	/**
	 * 
	 * 
	 * @param query
	 * @return true if no pb
	 */
	public int execSQL(String query) {
		Connection conn = null;
		int result = -1;
		
		try {
			conn = mDatabase.getConnection();
			Statement stat = conn.createStatement();
			
			result = stat.executeUpdate(query);

		} catch (SQLException e) {
			log.error("Raw Query failed :", e);
			result = -1;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					result = -1;
				}
			}
		}
		
		return result;
	}
	
	public PreparedStatement compileStatement(String query) {
		try {
			Connection conn = mDatabase.getConnection();
			conn.setAutoCommit(false);
			
			PreparedStatement stat = conn.prepareStatement(query);
			
			return new ConnectionAwarePreparedStatement(stat, conn);
		} catch (SQLException e) {
			log.error("Compile Statement failed failed :", e);
		}
		return null;
	}
	
	public void delete(String table, String whereClause) {
		
		if (StringUtils.isBlank(table) || StringUtils.isBlank(whereClause)) {
			log.error("DELETE failed : table and whereClause must not be blank {} {}", table, whereClause);
			return;
		}
		
		String query = "DELETE FROM " + table + " WHERE " + whereClause;
		execSQL(query);
	}
	
	public void update(String table, Map<String, Object> values, String whereClause) {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE ").append(table).append(" SET ");
		for (Entry<String, Object> entry : values.entrySet()) {
			sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append(", ");
		}
		sb.deleteCharAt(sb.length() - 2);
		sb.append("WHERE ").append(whereClause);
		execSQL(sb.toString());
	}
	
	
	/**
	 * Mapping of Java type names to the corresponding Cursor.get method.
	 * 
	 * @param typeName
	 *            The simple name of the type's class. Example:
	 *            String.class.getSimpleName().
	 * @return The name of the Cursor method to be called.
	 */
	private static String getCursorMethodName(String typeName) {
		if (typeName.equals("String")) {
			return "getString";
		} else if (typeName.equals("Long")) {
			return "getLong";
		} else if (typeName.equals("Integer")) {
			return "getInt";
		} else if (typeName.equals("Float")) {
			return "getFloat";
		} else if (typeName.equals("Double")) {
			return "getDouble";
		} else {
			return null;
		}
	}
}
