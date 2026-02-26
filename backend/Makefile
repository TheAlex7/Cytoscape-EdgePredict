CC = gcc
OBJ_DIR = mock/_objs

all: $(OBJ_DIR) $(OBJ_DIR)/hello $(OBJ_DIR)/mock_blant

$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

$(OBJ_DIR)/hello: mock/hello.c
	$(CC) $< -o $@

$(OBJ_DIR)/mock_blant: mock/mock_blant.c
	$(CC) $< -o $@

clean:
	rm -rf $(OBJ_DIR)

.PHONY: all clean