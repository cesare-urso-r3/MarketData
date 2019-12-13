val provider = rpcParty(10006)
val distributor = rpcParty(10009)
val subscriber = rpcParty(10012)
val distributor2 = rpcParty(10015)

val p = provider
val r = distributor
val s = subscriber
val r2 = distributor2

p.createTandC("provider2.zip")
p.createDataSet("LSE", "provider2.zip", 10.0)

r.downloadDataSet("LSE", provider)

r.createTandC("RedistributorDemoT&C.zip")
r.createDistributableDataSet("LSE", p, "RedistributorDemoT&C.zip", 1.0)

s.downloadDistributableDataSet("LSE", p, r)
s.signTandC("provider2.zip", p)
s.signTandC("RedistributorDemoT&C.zip", r)
s.requestPermission(provider, distributor, "LSE")

s.createUsage(provider, distributor, "LSE", "Adam.Houston")
s.createUsage(provider, distributor, "LSE", "Honest.Joe")
s.createUsage(provider, distributor, "LSE", "Dodgy.Dave")

r.createBill(subscriber, "2019-12-01", "2019-12-31")


