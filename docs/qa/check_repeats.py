import sqlite3
p = r"D:\Users\Lenovo\Desktop\JNSUB\test.db"
c = sqlite3.connect(p)
rows=c.execute("select station_name,line,checkin_group,checked_in_at from checkins order by checked_in_at").fetchall()
print("rows=",len(rows))
print("groups=",len(set(r[2] for r in rows)))
print(rows)
