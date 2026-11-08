import sqlite3
p = r"D:\Users\Lenovo\Desktop\JNSUB\test.db"
c = sqlite3.connect(p)
print(c.execute("select sql from sqlite_master where name='checkins'").fetchone()[0])
