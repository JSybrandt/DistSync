touch tmp.out
rm tmp.out
touch tmp.out

while read line
do
    stat --format="root|%i|%F|%s|%h|%U|%g|%f|%X|%Y|%o|%Z|$line" "$line" | awk 'BEGIN{FS="|";OFS="|";p=0}$3~/link/{$3="L";print $0;p++}$3~/dir/{$3="D";print $0;p++}$3~/file/{$3="F";print $0;p++}{if(p==0){$3="O";print $0;}}'  >> tmp.out
done

if [ $# -ge 1 ]
then
	sort --parallel=8 -S 20G -t "|" -k 13 -o $1 tmp.out
else 
	sort --parallel=8 -S 20G -t "|" -k 13  tmp.out
fi

rm tmp.out
