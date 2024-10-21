echo ""
echo "Install apt packeges:"
echo ""
bash ./restore_apt_packages.sh

echo ""
echo "create venv:"
echo ""
python -m venv ~/apk
source ~/apk/bin/activate

echo ""
echo "Install pip packeges:"
echo ""
bash ./restore_pip_packages.sh

bash ./build_apk.sh
