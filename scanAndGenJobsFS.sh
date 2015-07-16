cd expirement/fresh/
find . | ../../fakeScans/scanMaker.sh > ../../fakeScans/fresh.scan
cd ../stale/
find . | ../../fakeScans/scanMaker.sh > ../../fakeScans/stale.scan
cd ../../jobSplitter/
./compare -o ../fakeScans/stale.scan -n ../fakeScans/fresh.scan -f ../tmp/jobs/