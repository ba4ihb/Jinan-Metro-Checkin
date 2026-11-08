import sqlite3
p = r"D:\Users\Lenovo\Desktop\JNSUB\test.db"
c = sqlite3.connect(p)
print("checkins=", c.execute("select station_name,line,photo_count from checkins").fetchall())
print("schema=", c.execute("select sql from sqlite_master where name='achievements'").fetchone())
