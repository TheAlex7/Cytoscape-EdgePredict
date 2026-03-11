OBJ_DIR="native_src/mock/_objs"
MOCK_SRC_DIR="native_src/mock"

mkdir -p $OBJ_DIR
gcc $MOCK_SRC_DIR/mock_blant.c -o $OBJ_DIR/mock_blant
